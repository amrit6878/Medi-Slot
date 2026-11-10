package com.motherson.medislot.service;

import com.motherson.medislot.domain.*;
import com.motherson.medislot.dto.Dtos.*;

import java.util.List;

/** Entity -> DTO conversions. Call inside a transaction (lazy associations are read). */
public final class Mapper {
    private Mapper() {}

    public static DepartmentDto department(Department d) {
        return new DepartmentDto(d.getId(), d.getName());
    }

    public static HospitalRef hospitalRef(Hospital h) {
        return new HospitalRef(h.getId(), h.getName(), h.getCity());
    }

    public static ScheduleDto schedule(DoctorSchedule s) {
        return new ScheduleDto(s.getDayOfWeek(), s.getStartTime(), s.getEndTime(), s.getSlotDurationMinutes());
    }

    public static SlotDto slot(Slot s) {
        return new SlotDto(s.getId(), s.getSlotDate(), s.getStartTime(), s.getEndTime());
    }

    public static SuperHospitalDto superHospital(Hospital h) {
        return new SuperHospitalDto(h.getId(), h.getName(), h.getAddress(), h.getCity(), h.getPhone(), h.getEmail(),
                h.getOpeningTime(), h.getClosingTime(), h.isActive());
    }

    public static DoctorDto doctor(Doctor d, List<DoctorSchedule> schedules) {
        return new DoctorDto(d.getId(), d.getName(), d.getSpecialization(), d.getQualification(),
                d.getExperienceYears(), d.getConsultationFee(), d.getAbout(), d.getProfileImageUrl(),
                department(d.getDepartment()), hospitalRef(d.getHospital()),
                schedules == null ? null : schedules.stream().map(Mapper::schedule).toList(), null, null);
    }

    public static DoctorDto doctor(Doctor d, Boolean availableOnDate, java.time.LocalDate nextAvailableDate) {
        return new DoctorDto(d.getId(), d.getName(), d.getSpecialization(), d.getQualification(),
                d.getExperienceYears(), d.getConsultationFee(), d.getAbout(), d.getProfileImageUrl(),
                department(d.getDepartment()), hospitalRef(d.getHospital()), null,
                availableOnDate, nextAvailableDate);
    }

    public static AdminDoctorDto adminDoctor(Doctor d, List<DoctorSchedule> schedules) {
        return new AdminDoctorDto(d.getId(), d.getName(), d.getSpecialization(), d.getQualification(),
                d.getExperienceYears(), d.getEmail(), d.getPhone(), d.getConsultationFee(), d.getAbout(),
                d.getProfileImageUrl(), department(d.getDepartment()), d.isActive(),
                schedules.stream().map(Mapper::schedule).toList());
    }

    public static AppointmentDto appointment(Appointment a) {
        Doctor d = a.getDoctor();
        Employee e = a.getEmployee();
        return new AppointmentDto(a.getId(), a.getStatus().name(), a.getReason(), a.getCreatedAt(),
                new DoctorBrief(d.getId(), d.getName(), d.getSpecialization(), department(d.getDepartment())),
                hospitalRef(a.getHospital()),
                slot(a.getSlot()),
                new EmployeeBrief(e.getId(), e.getName(), e.getEmployeeCode(), e.getEmail()));
    }

    public static EmployeeDto employee(Employee e) {
        return new EmployeeDto(e.getId(), e.getName(), e.getEmployeeCode(), e.getEmail(), e.getPhone(),
                e.getPlantLocation());
    }

    public static AdminAccountDto adminAccount(HospitalAdmin a) {
        return new AdminAccountDto(a.getId(), a.getName(), a.getEmail(), a.getHospital().getId(),
                a.getHospital().getName(), a.isActive());
    }
}
