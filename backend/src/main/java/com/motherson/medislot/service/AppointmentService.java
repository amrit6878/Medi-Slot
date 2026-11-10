package com.motherson.medislot.service;

import com.motherson.medislot.domain.*;
import com.motherson.medislot.dto.Dtos.*;
import com.motherson.medislot.exception.ApiException;
import com.motherson.medislot.repository.AppointmentRepository;
import com.motherson.medislot.repository.SlotRepository;
import com.motherson.medislot.security.AuthUser;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class AppointmentService {
    private final AppointmentRepository appointments;
    private final SlotRepository slots;
    private final CurrentUserService current;
    private final Clock clock;

    public AppointmentService(AppointmentRepository appointments, SlotRepository slots,
                              CurrentUserService current, Clock clock) {
        this.appointments = appointments;
        this.slots = slots;
        this.current = current;
        this.clock = clock;
    }

    // =====================  admin reports  =====================

    @Transactional(readOnly = true)
    public ReportDto report(AuthUser user, LocalDate from, LocalDate to) {
        Long hospitalId = current.adminHospitalId(user);
        LocalDate today = LocalDate.now(clock);
        LocalDate end = to != null ? to : today;
        LocalDate start = from != null ? from : end.minusDays(29);
        if (start.isAfter(end)) throw ApiException.badRequest("The start date must not be after the end date.");
        if (start.plusDays(366).isBefore(end)) throw ApiException.badRequest("Please choose a range of at most one year.");

        long booked = 0, confirmed = 0, completed = 0, cancelled = 0, noShow = 0;
        java.util.Map<Long, long[]> perDoctor = new java.util.LinkedHashMap<>();   // total, completed, cancelled, noShow
        java.util.Map<Long, String[]> doctorInfo = new java.util.HashMap<>();
        java.util.Map<LocalDate, Long> perDay = new java.util.TreeMap<>();

        for (Object[] row : appointments.reportRows(hospitalId, start, end)) {
            LocalDate day = (LocalDate) row[0];
            Long doctorId = (Long) row[1];
            AppointmentStatus status = (AppointmentStatus) row[4];
            long n = ((Number) row[5]).longValue();

            doctorInfo.putIfAbsent(doctorId, new String[]{(String) row[2], (String) row[3]});
            long[] c = perDoctor.computeIfAbsent(doctorId, k -> new long[4]);
            c[0] += n;
            switch (status) {
                case BOOKED -> booked += n;
                case CONFIRMED -> confirmed += n;
                case COMPLETED -> { completed += n; c[1] += n; }
                case CANCELLED -> { cancelled += n; c[2] += n; }
                case NO_SHOW -> { noShow += n; c[3] += n; }
            }
            perDay.merge(day, n, Long::sum);
        }

        List<ReportDoctorRow> byDoctor = new ArrayList<>();
        perDoctor.forEach((id, c) -> byDoctor.add(new ReportDoctorRow(id, doctorInfo.get(id)[0], doctorInfo.get(id)[1],
                c[0], c[1], c[2], c[3])));
        byDoctor.sort(java.util.Comparator.comparingLong(ReportDoctorRow::total).reversed());
        List<ReportDayRow> byDay = new ArrayList<>();
        perDay.forEach((d, n) -> byDay.add(new ReportDayRow(d, n)));

        long total = booked + confirmed + completed + cancelled + noShow;
        return new ReportDto(current.hospitalName(user), start, end,
                new ReportSummary(total, booked, confirmed, completed, cancelled, noShow), byDoctor, byDay);
    }

    // =====================  employee  =====================

    @Transactional
    public AppointmentDto book(AuthUser user, BookRequest req) {
        Employee employee = current.employee(user);
        // Row lock: concurrent bookings of the same slot are serialised here.
        Slot slot = slots.findByIdForUpdate(req.slotId())
                .orElseThrow(() -> ApiException.notFound("That slot does not exist."));
        Doctor doctor = slot.getDoctor();
        Hospital hospital = doctor.getHospital();

        if (!doctor.isActive() || !hospital.isActive()) {
            throw ApiException.conflict("This doctor is not accepting appointments right now.");
        }
        if (hasStarted(slot)) throw ApiException.conflict("That time has already passed. Please choose another slot.");
        if (slot.isBooked()) throw ApiException.conflict("That slot was just booked by someone else. Please choose another.");
        if (appointments.countOverlapping(employee.getId(), AppointmentStatus.LIVE,
                slot.getSlotDate(), slot.getStartTime(), slot.getEndTime()) > 0) {
            throw ApiException.conflict("You already have an appointment at this time.");
        }

        slot.setBooked(true);
        Appointment a = new Appointment();
        a.setEmployee(employee);
        a.setDoctor(doctor);
        a.setHospital(hospital);
        a.setSlot(slot);
        a.setStatus(AppointmentStatus.BOOKED);
        String reason = req.reason() == null ? null : req.reason().trim();
        a.setReason(reason == null || reason.isEmpty() ? null : reason);
        appointments.save(a);
        return Mapper.appointment(a);
    }

    /** @param status "upcoming" (default) or "past" */
    @Transactional(readOnly = true)
    public List<AppointmentDto> mine(AuthUser user, String status) {
        LocalDate today = LocalDate.now(clock);
        LocalTime now = LocalTime.now(clock);
        List<Appointment> rows = "past".equalsIgnoreCase(status)
                ? appointments.findPast(user.id(), AppointmentStatus.LIVE, today, now)
                : appointments.findUpcoming(user.id(), AppointmentStatus.LIVE, today, now);
        return rows.stream().map(Mapper::appointment).toList();
    }

    @Transactional(readOnly = true)
    public AppointmentDto getMine(AuthUser user, Long id) {
        Appointment a = appointments.findDetailedById(id)
                .filter(x -> x.getEmployee().getId().equals(user.id()))
                .orElseThrow(() -> ApiException.notFound("Appointment not found."));
        return Mapper.appointment(a);
    }

    @Transactional
    public AppointmentDto cancelMine(AuthUser user, Long id) {
        Appointment a = appointments.findDetailedById(id)
                .filter(x -> x.getEmployee().getId().equals(user.id()))
                .orElseThrow(() -> ApiException.notFound("Appointment not found."));
        if (!AppointmentStatus.LIVE.contains(a.getStatus())) {
            throw ApiException.conflict("Only upcoming appointments can be cancelled.");
        }
        if (hasStarted(a.getSlot())) throw ApiException.conflict("This appointment time has already started.");
        cancel(a, "EMPLOYEE");
        return Mapper.appointment(a);
    }

    // =====================  hospital admin  =====================

    @Transactional(readOnly = true)
    public AdminDashboardDto dashboard(AuthUser user) {
        Long hospitalId = current.adminHospitalId(user);
        LocalDate today = LocalDate.now(clock);
        return new AdminDashboardDto(
                current.hospitalName(user),
                appointments.countOnDateExcluding(hospitalId, today, AppointmentStatus.CANCELLED),
                appointments.countByHospitalAndStatus(hospitalId, AppointmentStatus.BOOKED),
                appointments.countOnDateWithStatus(hospitalId, today, AppointmentStatus.COMPLETED),
                slots.countDoctorsWithSlotsOn(hospitalId, today));
    }

    @Transactional(readOnly = true)
    public PageResponse<AppointmentDto> adminList(AuthUser user, LocalDate date, Long doctorId,
                                                  AppointmentStatus status, int page, int size) {
        Long hospitalId = current.adminHospitalId(user);
        Specification<Appointment> spec = (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            p.add(cb.equal(root.get("hospital").get("id"), hospitalId));
            if (date != null) p.add(cb.equal(root.get("slot").get("slotDate"), date));
            if (doctorId != null) p.add(cb.equal(root.get("doctor").get("id"), doctorId));
            if (status != null) p.add(cb.equal(root.get("status"), status));
            return cb.and(p.toArray(new Predicate[0]));
        };
        Page<Appointment> result = appointments.findAll(spec, PageRequest.of(
                Math.max(0, page), Math.min(Math.max(1, size), 100),
                Sort.by(Sort.Order.desc("slot.slotDate"), Sort.Order.desc("slot.startTime"))));
        return new PageResponse<>(result.getContent().stream().map(Mapper::appointment).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    /** action: confirm | cancel | complete | no-show */
    @Transactional
    public AppointmentDto adminTransition(AuthUser user, Long id, String action) {
        Long hospitalId = current.adminHospitalId(user);
        Appointment a = appointments.findDetailedById(id)
                .filter(x -> x.getHospital().getId().equals(hospitalId))
                .orElseThrow(() -> ApiException.notFound("Appointment not found."));
        AppointmentStatus s = a.getStatus();
        boolean live = AppointmentStatus.LIVE.contains(s);

        switch (action.toLowerCase()) {
            case "confirm" -> {
                if (s != AppointmentStatus.BOOKED) throw ApiException.conflict("Only booked appointments can be confirmed.");
                if (hasEnded(a.getSlot())) throw ApiException.conflict("This appointment time has already passed.");
                a.setStatus(AppointmentStatus.CONFIRMED);
            }
            case "cancel" -> {
                if (!live) throw ApiException.conflict("This appointment can no longer be cancelled.");
                cancel(a, "ADMIN");
            }
            case "complete" -> {
                if (!live) throw ApiException.conflict("Only booked or confirmed appointments can be completed.");
                a.setStatus(AppointmentStatus.COMPLETED);
            }
            case "no-show" -> {
                if (!live) throw ApiException.conflict("Only booked or confirmed appointments can be marked no-show.");
                if (!hasStarted(a.getSlot())) throw ApiException.conflict("The appointment has not started yet.");
                a.setStatus(AppointmentStatus.NO_SHOW);
            }
            default -> throw ApiException.badRequest("Unknown action '" + action + "'.");
        }
        return Mapper.appointment(a);
    }

    // =====================  scheduler  =====================

    /** Live appointments whose slot has ended become COMPLETED, i.e. they move to the employee's "Past" tab. */
    @Transactional
    public int completeFinished() {
        return appointments.completeFinished(AppointmentStatus.COMPLETED, AppointmentStatus.LIVE,
                Instant.now(), LocalDate.now(clock), LocalTime.now(clock));
    }

    // =====================  helpers  =====================

    private void cancel(Appointment a, String by) {
        a.setStatus(AppointmentStatus.CANCELLED);
        a.setCancelledBy(by);
        a.getSlot().setBooked(false);     // the slot becomes bookable again
    }

    private boolean hasStarted(Slot s) {
        LocalDate today = LocalDate.now(clock);
        return s.getSlotDate().isBefore(today)
                || (s.getSlotDate().isEqual(today) && !s.getStartTime().isAfter(LocalTime.now(clock)));
    }

    private boolean hasEnded(Slot s) {
        LocalDate today = LocalDate.now(clock);
        return s.getSlotDate().isBefore(today)
                || (s.getSlotDate().isEqual(today) && !s.getEndTime().isAfter(LocalTime.now(clock)));
    }
}
