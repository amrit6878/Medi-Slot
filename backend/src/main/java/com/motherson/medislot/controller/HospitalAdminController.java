package com.motherson.medislot.controller;

import com.motherson.medislot.domain.AppointmentStatus;
import com.motherson.medislot.dto.Dtos.*;
import com.motherson.medislot.repository.DepartmentRepository;
import com.motherson.medislot.security.AuthUser;
import com.motherson.medislot.service.*;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/** Everything a hospital admin (role ADMIN) can do - always scoped to THEIR hospital. */
@RestController
@RequestMapping("/admin")
public class HospitalAdminController {
    private final AppointmentService appointments;
    private final DoctorService doctors;
    private final HospitalService hospitals;
    private final DepartmentRepository departments;
    private final AdminAccountService account;

    public HospitalAdminController(AppointmentService appointments, DoctorService doctors,
                                   HospitalService hospitals, DepartmentRepository departments,
                                   AdminAccountService account) {
        this.account = account;
        this.appointments = appointments;
        this.doctors = doctors;
        this.hospitals = hospitals;
        this.departments = departments;
    }

    @GetMapping("/dashboard")
    public AdminDashboardDto dashboard(@AuthenticationPrincipal AuthUser user) {
        return appointments.dashboard(user);
    }

    // ----- hospital profile -----
    @GetMapping("/hospital")
    public SuperHospitalDto hospital(@AuthenticationPrincipal AuthUser user) {
        return hospitals.myHospital(user);
    }

    @PutMapping("/hospital")
    public SuperHospitalDto updateHospital(@AuthenticationPrincipal AuthUser user, @Valid @RequestBody HospitalRequest request) {
        return hospitals.updateMyHospital(user, request);
    }

    // ----- doctors -----
    @GetMapping("/departments")
    public List<DepartmentDto> departments() {
        return departments.findAllByOrderByNameAsc().stream().map(Mapper::department).toList();
    }

    @GetMapping("/doctors")
    public List<AdminDoctorDto> doctors(@AuthenticationPrincipal AuthUser user,
                                        @RequestParam(required = false) String name,
                                        @RequestParam(required = false) Long departmentId,
                                        @RequestParam(required = false) Boolean isActive) {
        return doctors.adminList(user, name, departmentId, isActive);
    }

    @GetMapping("/doctors/{id}")
    public AdminDoctorDto doctor(@AuthenticationPrincipal AuthUser user, @PathVariable Long id) {
        return doctors.adminGet(user, id);
    }

    @PostMapping("/doctors")
    public AdminDoctorDto createDoctor(@AuthenticationPrincipal AuthUser user, @Valid @RequestBody DoctorRequest request) {
        return doctors.create(user, request);
    }

    @PutMapping("/doctors/{id}")
    public AdminDoctorDto updateDoctor(@AuthenticationPrincipal AuthUser user, @PathVariable Long id,
                                       @Valid @RequestBody DoctorRequest request) {
        return doctors.update(user, id, request);
    }

    @PutMapping("/doctors/{id}/status")
    public AdminDoctorDto doctorStatus(@AuthenticationPrincipal AuthUser user, @PathVariable Long id,
                                       @Valid @RequestBody StatusRequest request) {
        return doctors.setStatus(user, id, request.isActive());
    }

    @PutMapping("/doctors/{id}/schedule")
    public ScheduleResult doctorSchedule(@AuthenticationPrincipal AuthUser user, @PathVariable Long id,
                                         @Valid @RequestBody ScheduleUpdateRequest request) {
        return doctors.replaceSchedule(user, id, request);
    }

    // ----- appointments -----
    @GetMapping("/appointments")
    public PageResponse<AppointmentDto> appointments(
            @AuthenticationPrincipal AuthUser user,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Long doctorId,
            @RequestParam(required = false) AppointmentStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return appointments.adminList(user, date, doctorId, status, page, size);
    }

    /** action = confirm | cancel | complete | no-show */
    @PutMapping("/appointments/{id}/{action}")
    public AppointmentDto transition(@AuthenticationPrincipal AuthUser user, @PathVariable Long id,
                                     @PathVariable String action) {
        return appointments.adminTransition(user, id, action);
    }

    // ----- reports -----
    @GetMapping("/reports")
    public ReportDto report(@AuthenticationPrincipal AuthUser user,
                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return appointments.report(user, from, to);
    }

    // ----- settings (own account) -----
    @GetMapping("/settings")
    public AdminSettingsDto settings(@AuthenticationPrincipal AuthUser user) {
        return account.get(user);
    }

    @PutMapping("/settings/profile")
    public AdminSettingsDto updateProfile(@AuthenticationPrincipal AuthUser user,
                                          @Valid @RequestBody ProfileUpdateRequest request) {
        return account.updateName(user, request.name());
    }

    @PutMapping("/settings/password")
    public MessageDto changePassword(@AuthenticationPrincipal AuthUser user,
                                     @Valid @RequestBody PasswordChangeRequest request) {
        account.changePassword(user, request.currentPassword(), request.newPassword());
        return new MessageDto("Password updated.");
    }
}
