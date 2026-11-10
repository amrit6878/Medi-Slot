package com.motherson.medislot.service;

import com.motherson.medislot.domain.Department;
import com.motherson.medislot.domain.Doctor;
import com.motherson.medislot.domain.DoctorSchedule;
import com.motherson.medislot.dto.Dtos.*;
import com.motherson.medislot.exception.ApiException;
import com.motherson.medislot.repository.DepartmentRepository;
import com.motherson.medislot.repository.DoctorRepository;
import com.motherson.medislot.repository.DoctorScheduleRepository;
import com.motherson.medislot.repository.HospitalRepository;
import com.motherson.medislot.repository.SlotRepository;
import com.motherson.medislot.security.AuthUser;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DoctorService {
    private static final Set<Integer> ALLOWED_DURATIONS_MIN = Set.of(5, 10, 15, 20, 30, 45, 60, 90, 120);

    private final DoctorRepository doctors;
    private final DoctorScheduleRepository schedules;
    private final DepartmentRepository departments;
    private final HospitalRepository hospitals;
    private final SlotRepository slots;
    private final SlotService slotService;
    private final CurrentUserService current;
    private final Clock clock;

    public DoctorService(DoctorRepository doctors, DoctorScheduleRepository schedules,
                         DepartmentRepository departments, HospitalRepository hospitals, SlotRepository slots,
                         SlotService slotService, CurrentUserService current, Clock clock) {
        this.doctors = doctors;
        this.schedules = schedules;
        this.departments = departments;
        this.hospitals = hospitals;
        this.slots = slots;
        this.slotService = slotService;
        this.current = current;
        this.clock = clock;
    }

    // =====================  employee side  =====================

    /**
     * Active doctors of active hospitals. When a date is given every doctor is still returned, each flagged with
     * availableOnDate and nextAvailableDate, so a newly added doctor never "disappears" just because the chosen day
     * (e.g. today after hours, or a weekend) has no free slot. onlyAvailable=true restores the strict filter.
     */
    @Transactional(readOnly = true)
    public List<DoctorDto> search(Long hospitalId, Long departmentId, String name, LocalDate date, boolean onlyAvailable) {
        Specification<Doctor> spec = (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            p.add(cb.isTrue(root.get("active")));
            p.add(cb.isTrue(root.get("hospital").get("active")));
            if (hospitalId != null) p.add(cb.equal(root.get("hospital").get("id"), hospitalId));
            if (departmentId != null) p.add(cb.equal(root.get("department").get("id"), departmentId));
            if (name != null && !name.isBlank()) {
                p.add(cb.like(cb.lower(root.get("name")), "%" + name.trim().toLowerCase() + "%"));
            }
            return cb.and(p.toArray(new Predicate[0]));
        };
        List<Doctor> result = doctors.findAll(spec, Sort.by("name"));

        if (date == null || result.isEmpty()) {
            return result.stream().map(d -> Mapper.doctor(d, null)).toList();
        }
        LocalDate today = LocalDate.now(clock);
        LocalTime now = LocalTime.now(clock);
        LocalDate from = date.isBefore(today) ? today : date;
        Set<Long> available = new HashSet<>(slots.doctorIdsWithOpenSlots(from, today, now));
        java.util.Map<Long, LocalDate> next = new java.util.HashMap<>();
        for (Object[] row : slots.nextAvailableDates(result.stream().map(Doctor::getId).toList(), from, today, now)) {
            next.put((Long) row[0], (LocalDate) row[1]);
        }
        List<DoctorDto> out = new ArrayList<>();
        for (Doctor d : result) {
            boolean free = available.contains(d.getId());
            if (onlyAvailable && !free) continue;
            out.add(Mapper.doctor(d, free, next.get(d.getId())));
        }
        // doctors who can be booked on the chosen day first, then by name (sort is stable)
        out.sort(java.util.Comparator.comparing((DoctorDto x) -> !Boolean.TRUE.equals(x.availableOnDate())));
        return out;
    }

    @Transactional(readOnly = true)
    public DoctorDto detail(Long id) {
        Doctor d = doctors.findDetailedById(id).orElseThrow(() -> ApiException.notFound("Doctor not found."));
        if (!d.isActive() || !d.getHospital().isActive()) throw ApiException.notFound("Doctor not found.");
        return Mapper.doctor(d, schedules.findByDoctorIdOrderByDayOfWeekAsc(id));
    }

    // =====================  hospital admin side  =====================

    @Transactional(readOnly = true)
    public List<AdminDoctorDto> adminList(AuthUser user, String name, Long departmentId, Boolean isActive) {
        Long hospitalId = current.adminHospitalId(user);
        List<Doctor> list = doctors.findByHospitalIdOrderByNameAsc(hospitalId).stream()
                .filter(d -> name == null || name.isBlank() || d.getName().toLowerCase().contains(name.trim().toLowerCase()))
                .filter(d -> departmentId == null || d.getDepartment().getId().equals(departmentId))
                .filter(d -> isActive == null || d.isActive() == isActive)
                .toList();
        Map<Long, List<DoctorSchedule>> byDoctor = list.isEmpty() ? Map.of()
                : schedules.findByDoctorIds(list.stream().map(Doctor::getId).toList()).stream()
                .collect(Collectors.groupingBy(s -> s.getDoctor().getId()));
        return list.stream()
                .map(d -> Mapper.adminDoctor(d, byDoctor.getOrDefault(d.getId(), List.of())))
                .toList();
    }

    @Transactional(readOnly = true)
    public AdminDoctorDto adminGet(AuthUser user, Long id) {
        Doctor d = ownedDoctor(user, id);
        return Mapper.adminDoctor(d, schedules.findByDoctorIdOrderByDayOfWeekAsc(id));
    }

    @Transactional
    public AdminDoctorDto create(AuthUser user, DoctorRequest req) {
        Long hospitalId = current.adminHospitalId(user);
        Doctor doctor = new Doctor();
        doctor.setHospital(hospitals.getReferenceById(hospitalId));
        apply(doctor, req);
        doctor.setActive(req.active() == null || req.active());
        doctors.save(doctor);

        List<Integer> days = req.workingDays() == null ? List.of() : req.workingDays().stream().distinct().sorted().toList();
        if (!days.isEmpty()) {
            LocalTime start = req.startTime() != null ? req.startTime() : LocalTime.of(9, 0);
            LocalTime end = req.endTime() != null ? req.endTime() : LocalTime.of(17, 0);
            int duration = req.slotDurationMinutes() != null ? req.slotDurationMinutes() : 30;
            List<DoctorSchedule> rows = new ArrayList<>();
            for (Integer day : days) rows.add(buildSchedule(doctor, day, start, end, duration));
            schedules.saveAll(rows);
            slotService.generateFor(doctor.getId());
        }
        return Mapper.adminDoctor(doctor, schedules.findByDoctorIdOrderByDayOfWeekAsc(doctor.getId()));
    }

    @Transactional
    public AdminDoctorDto update(AuthUser user, Long id, DoctorRequest req) {
        Doctor doctor = ownedDoctor(user, id);
        apply(doctor, req);
        if (req.active() != null) doctor.setActive(req.active());
        return Mapper.adminDoctor(doctor, schedules.findByDoctorIdOrderByDayOfWeekAsc(id));
    }

    @Transactional
    public AdminDoctorDto setStatus(AuthUser user, Long id, boolean active) {
        Doctor doctor = ownedDoctor(user, id);
        doctor.setActive(active);
        doctors.flush();
        if (active) slotService.generateFor(id);   // refill slots for days missed while inactive
        return Mapper.adminDoctor(doctor, schedules.findByDoctorIdOrderByDayOfWeekAsc(id));
    }

    /**
     * Replaces the weekly availability. Unbooked future slots are rebuilt from the new schedule so employees
     * see the change immediately; slots that already carry an appointment are kept.
     */
    @Transactional
    public ScheduleResult replaceSchedule(AuthUser user, Long id, ScheduleUpdateRequest req) {
        Doctor doctor = ownedDoctor(user, id);

        Set<Integer> seen = new HashSet<>();
        List<DoctorSchedule> rows = new ArrayList<>();
        for (ScheduleRequest r : req.schedules()) {
            if (!seen.add(r.dayOfWeek())) throw ApiException.badRequest("Each weekday can appear only once.");
            rows.add(buildSchedule(doctor, r.dayOfWeek(), r.startTime(), r.endTime(), r.slotDurationMinutes()));
        }

        slotService.clearOpenSlots(id);
        schedules.deleteAllForDoctor(id);
        schedules.saveAll(rows);
        schedules.flush();
        int created = slotService.generateFor(id);

        return new ScheduleResult(created,
                schedules.findByDoctorIdOrderByDayOfWeekAsc(id).stream().map(Mapper::schedule).toList());
    }

    // =====================  helpers  =====================

    private Doctor ownedDoctor(AuthUser user, Long id) {
        Long hospitalId = current.adminHospitalId(user);
        return doctors.findByIdAndHospitalId(id, hospitalId)
                .orElseThrow(() -> ApiException.notFound("Doctor not found in your hospital."));
    }

    private void apply(Doctor d, DoctorRequest req) {
        Department department = departments.findById(req.departmentId())
                .orElseThrow(() -> ApiException.badRequest("Selected department does not exist."));
        d.setName(req.name().trim());
        d.setDepartment(department);
        d.setSpecialization(req.specialization().trim());
        d.setQualification(blankToNull(req.qualification()));
        d.setExperienceYears(req.experienceYears());
        d.setEmail(blankToNull(req.email()));
        d.setPhone(blankToNull(req.phone()));
        d.setConsultationFee(req.consultationFee());
        d.setAbout(blankToNull(req.about()));
        d.setProfileImageUrl(blankToNull(req.profileImageUrl()));
    }

    private DoctorSchedule buildSchedule(Doctor doctor, int day, LocalTime start, LocalTime end, int duration) {
        if (day < 1 || day > 7) throw ApiException.badRequest("Weekday must be between 1 (Mon) and 7 (Sun).");
        if (!end.isAfter(start)) throw ApiException.badRequest("End time must be after start time.");
        if (!ALLOWED_DURATIONS_MIN.contains(duration)) {
            throw ApiException.badRequest("Slot duration must be one of " + new TreeSet<>(ALLOWED_DURATIONS_MIN) + " minutes.");
        }
        if (java.time.Duration.between(start, end).toMinutes() < duration) {
            throw ApiException.badRequest("The working window is shorter than one slot.");
        }
        DoctorSchedule s = new DoctorSchedule();
        s.setDoctor(doctor);
        s.setDayOfWeek(day);
        s.setStartTime(start);
        s.setEndTime(end);
        s.setSlotDurationMinutes(duration);
        return s;
    }

    private static String blankToNull(String v) {
        return v == null || v.isBlank() ? null : v.trim();
    }
}
