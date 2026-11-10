package com.motherson.medislot.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * All request/response shapes in one place. Field names match what the React client
 * (src/api.js and the pages) already reads, so no UI change is needed.
 */
public final class Dtos {
    private Dtos() {}

    // ---------- auth ----------
    public record LoginRequest(@NotBlank String identifier, @NotBlank String password) {}

    public record UserInfo(Long id, String name, String email, String employeeCode,
                           Long hospitalId, String hospitalName, String role) {}

    public record LoginResponse(String token, String role, long expiresInSeconds, UserInfo user) {}

    // ---------- shared ----------
    public record DepartmentDto(Long id, String name) {}

    public record HospitalRef(Long id, String name, String city) {}

    public record SlotDto(Long id, LocalDate date, LocalTime startTime, LocalTime endTime) {}

    public record ScheduleDto(int dayOfWeek, LocalTime startTime, LocalTime endTime, int slotDurationMinutes) {}

    public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {}

    // ---------- hospitals ----------
    public record HospitalDto(Long id, String name, String address, String city, String phone, String email,
                              LocalTime openingTime, LocalTime closingTime, long doctorCount) {}

    public record HospitalDetailDto(Long id, String name, String address, String city, String phone, String email,
                                    LocalTime openingTime, LocalTime closingTime, List<DepartmentDto> departments) {}

    public record SuperHospitalDto(Long id, String name, String address, String city, String phone, String email,
                                   LocalTime openingTime, LocalTime closingTime, boolean active) {}

    public record HospitalRequest(
            @NotBlank @Size(max = 200) String name,
            @NotBlank @Size(max = 300) String address,
            @NotBlank @Size(max = 100) String city,
            @NotBlank @Size(max = 30) String phone,
            @NotBlank @Email @Size(max = 200) String email,
            @NotNull LocalTime openingTime,
            @NotNull LocalTime closingTime) {}

    // ---------- doctors ----------
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record DoctorDto(Long id, String name, String specialization, String qualification, int experienceYears,
                            BigDecimal consultationFee, String about, String profileImageUrl,
                            DepartmentDto department, HospitalRef hospital, List<ScheduleDto> weeklyTimings,
                            Boolean availableOnDate, LocalDate nextAvailableDate) {}

    public record AdminDoctorDto(Long id, String name, String specialization, String qualification,
                                 int experienceYears, String email, String phone, BigDecimal consultationFee,
                                 String about, String profileImageUrl, DepartmentDto department,
                                 @JsonProperty("isActive") boolean isActive, List<ScheduleDto> weeklyTimings) {}

    public record DoctorRequest(
            @NotBlank @Size(max = 150) String name,
            @NotNull Long departmentId,
            @NotBlank @Size(max = 150) String specialization,
            @Size(max = 200) String qualification,
            @NotNull @Min(0) @Max(70) Integer experienceYears,
            @Size(max = 200) String email,
            @Size(max = 30) String phone,
            @DecimalMin("0.0") BigDecimal consultationFee,
            @Size(max = 2000) String about,
            @Size(max = 500) String profileImageUrl,
            List<Integer> workingDays,
            LocalTime startTime,
            LocalTime endTime,
            Integer slotDurationMinutes,
            Boolean active) {}

    public record StatusRequest(@JsonProperty("isActive") @NotNull Boolean isActive) {}

    public record ScheduleRequest(
            @NotNull @Min(1) @Max(7) Integer dayOfWeek,
            @NotNull LocalTime startTime,
            @NotNull LocalTime endTime,
            @NotNull Integer slotDurationMinutes) {}

    public record ScheduleUpdateRequest(@NotNull @Valid List<ScheduleRequest> schedules) {}

    public record ScheduleResult(int slotsCreated, List<ScheduleDto> weeklyTimings) {}

    // ---------- appointments ----------
    public record BookRequest(@NotNull Long slotId, @Size(max = 500) String reason) {}

    public record DoctorBrief(Long id, String name, String specialization, DepartmentDto department) {}

    public record EmployeeBrief(Long id, String name, String employeeCode, String email) {}

    public record AppointmentDto(Long id, String status, String reason, Instant createdAt,
                                 DoctorBrief doctor, HospitalRef hospital, SlotDto slot, EmployeeBrief employee) {}

    public record AdminDashboardDto(String hospitalName, long todayAppointments, long pendingAppointments,
                                    long completedToday, long doctorsAvailableToday) {}

    // ---------- admin reports & settings ----------
    public record ReportSummary(long total, long booked, long confirmed, long completed, long cancelled, long noShow) {}

    public record ReportDoctorRow(Long doctorId, String doctorName, String department,
                                  long total, long completed, long cancelled, long noShow) {}

    public record ReportDayRow(LocalDate date, long total) {}

    public record ReportDto(String hospitalName, LocalDate from, LocalDate to, ReportSummary summary,
                            List<ReportDoctorRow> byDoctor, List<ReportDayRow> byDay) {}

    public record AdminSettingsDto(String name, String email, String hospitalName) {}

    public record ProfileUpdateRequest(@NotBlank @Size(max = 150) String name) {}

    public record PasswordChangeRequest(@NotBlank String currentPassword,
                                        @NotBlank @Size(min = 6, max = 100) String newPassword) {}

    public record MessageDto(String message) {}

    // ---------- employee ----------
    public record EmployeeDto(Long id, String name, String employeeCode, String email, String phone,
                              String plantLocation) {}

    public record EmployeeUpdateRequest(
            @NotBlank @Size(max = 150) String name,
            @Pattern(regexp = "^$|^[+0-9 ()-]{7,20}$", message = "must be a valid phone number") String phone,
            @Size(max = 150) String plantLocation) {}

    // ---------- super admin ----------
    public record SuperDashboardDto(long hospitals, long activeHospitals, long hospitalAdmins,
                                    long doctors, long appointments) {}

    public record AdminAccountDto(Long id, String name, String email, Long hospitalId, String hospitalName,
                                  boolean active) {}

    public record AdminAccountRequest(
            @NotNull Long hospitalId,
            @NotBlank @Size(max = 150) String name,
            @NotBlank @Email @Size(max = 200) String email,
            @Size(min = 6, max = 100) String password,
            Boolean active) {}
}
