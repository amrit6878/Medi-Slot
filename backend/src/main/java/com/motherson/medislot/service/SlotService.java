package com.motherson.medislot.service;

import com.motherson.medislot.config.AppProperties;
import com.motherson.medislot.domain.Doctor;
import com.motherson.medislot.domain.DoctorSchedule;
import com.motherson.medislot.domain.Slot;
import com.motherson.medislot.dto.Dtos.SlotDto;
import com.motherson.medislot.exception.ApiException;
import com.motherson.medislot.repository.DoctorRepository;
import com.motherson.medislot.repository.DoctorScheduleRepository;
import com.motherson.medislot.repository.SlotRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Turns each doctor's weekly schedule into concrete bookable Slot rows for the next N days. */
@Service
public class SlotService {
    private final SlotRepository slots;
    private final DoctorRepository doctors;
    private final DoctorScheduleRepository schedules;
    private final AppProperties props;
    private final Clock clock;

    public SlotService(SlotRepository slots, DoctorRepository doctors, DoctorScheduleRepository schedules,
                       AppProperties props, Clock clock) {
        this.slots = slots;
        this.doctors = doctors;
        this.schedules = schedules;
        this.props = props;
        this.clock = clock;
    }

    /** Slots an employee can still book for a doctor on a date. */
    @Transactional(readOnly = true)
    public List<SlotDto> openSlots(Long doctorId, LocalDate date) {
        Doctor doctor = doctors.findById(doctorId).orElseThrow(() -> ApiException.notFound("Doctor not found."));
        if (!doctor.isActive() || !doctor.getHospital().isActive()) return List.of();
        LocalDate today = LocalDate.now(clock);
        LocalDate target = date == null ? today : date;
        if (target.isBefore(today)) return List.of();
        return slots.findOpenSlots(doctorId, target, today, LocalTime.now(clock)).stream().map(Mapper::slot).toList();
    }

    /**
     * Creates the missing slots for today .. today+horizon-1 from the doctor's weekly schedule.
     * Existing slots (booked or not) are never touched, and a new slot is skipped if it would overlap one.
     *
     * @return number of slots created
     */
    @Transactional
    public int generateFor(Long doctorId) {
        Doctor doctor = doctors.findById(doctorId).orElseThrow(() -> ApiException.notFound("Doctor not found."));
        if (!doctor.isActive()) return 0;

        Map<Integer, DoctorSchedule> byDay = new HashMap<>();
        for (DoctorSchedule s : schedules.findByDoctorIdOrderByDayOfWeekAsc(doctorId)) byDay.put(s.getDayOfWeek(), s);
        if (byDay.isEmpty()) return 0;

        LocalDate today = LocalDate.now(clock);
        LocalDate last = today.plusDays(Math.max(1, props.slotHorizonDays()) - 1L);

        Map<LocalDate, List<Slot>> existing = new HashMap<>();
        for (Slot s : slots.findInRange(doctorId, today, last)) {
            existing.computeIfAbsent(s.getSlotDate(), k -> new ArrayList<>()).add(s);
        }

        List<Slot> created = new ArrayList<>();
        for (LocalDate day = today; !day.isAfter(last); day = day.plusDays(1)) {
            DoctorSchedule sch = byDay.get(day.getDayOfWeek().getValue());
            if (sch == null) continue;
            int duration = sch.getSlotDurationMinutes();
            int endMinute = sch.getEndTime().toSecondOfDay() / 60;
            List<Slot> sameDay = existing.getOrDefault(day, List.of());
            for (int m = sch.getStartTime().toSecondOfDay() / 60; m + duration <= endMinute; m += duration) {
                LocalTime start = LocalTime.ofSecondOfDay(m * 60L);
                int endM = m + duration;
                // a slot ending exactly at midnight is stored as 23:59:59 (LocalTime has no 24:00)
                LocalTime end = endM >= 1440 ? LocalTime.of(23, 59, 59) : LocalTime.ofSecondOfDay(endM * 60L);
                boolean overlaps = sameDay.stream()
                        .anyMatch(x -> start.isBefore(x.getEndTime()) && end.isAfter(x.getStartTime()));
                if (overlaps) continue;
                Slot slot = new Slot();
                slot.setDoctor(doctor);
                slot.setSlotDate(day);
                slot.setStartTime(start);
                slot.setEndTime(end);
                created.add(slot);
            }
        }
        slots.saveAll(created);
        return created.size();
    }

    /** Drops unbooked future slots (booked ones are kept so existing appointments stay valid). */
    @Transactional
    public void clearOpenSlots(Long doctorId) {
        slots.deleteOpenSlotsFrom(doctorId, LocalDate.now(clock));
    }
}
