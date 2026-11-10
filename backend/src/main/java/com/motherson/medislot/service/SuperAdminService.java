package com.motherson.medislot.service;

import com.motherson.medislot.domain.Hospital;
import com.motherson.medislot.domain.HospitalAdmin;
import com.motherson.medislot.dto.Dtos.*;
import com.motherson.medislot.exception.ApiException;
import com.motherson.medislot.repository.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Motherson-side control: partner hospitals and their administrator credentials. */
@Service
public class SuperAdminService {
    private final HospitalRepository hospitals;
    private final HospitalAdminRepository admins;
    private final SuperAdminRepository superAdmins;
    private final EmployeeRepository employees;
    private final DoctorRepository doctors;
    private final AppointmentRepository appointments;
    private final PasswordEncoder encoder;

    public SuperAdminService(HospitalRepository hospitals, HospitalAdminRepository admins,
                             SuperAdminRepository superAdmins, EmployeeRepository employees,
                             DoctorRepository doctors, AppointmentRepository appointments, PasswordEncoder encoder) {
        this.hospitals = hospitals;
        this.admins = admins;
        this.superAdmins = superAdmins;
        this.employees = employees;
        this.doctors = doctors;
        this.appointments = appointments;
        this.encoder = encoder;
    }

    // ---------- dashboard ----------
    @Transactional(readOnly = true)
    public SuperDashboardDto dashboard() {
        return new SuperDashboardDto(hospitals.count(), hospitals.countByActiveTrue(), admins.count(),
                doctors.count(), appointments.count());
    }

    // ---------- hospitals ----------
    @Transactional(readOnly = true)
    public List<SuperHospitalDto> hospitals() {
        return hospitals.findAllByOrderByNameAsc().stream().map(Mapper::superHospital).toList();
    }

    @Transactional
    public SuperHospitalDto createHospital(HospitalRequest req) {
        validateHours(req);
        if (hospitals.existsByNameIgnoreCaseAndCityIgnoreCase(req.name().trim(), req.city().trim())) {
            throw ApiException.conflict("A hospital with this name already exists in " + req.city().trim() + ".");
        }
        Hospital h = new Hospital();
        copy(h, req);
        return Mapper.superHospital(hospitals.save(h));
    }

    @Transactional
    public SuperHospitalDto updateHospital(Long id, HospitalRequest req) {
        validateHours(req);
        Hospital h = hospitals.findById(id).orElseThrow(() -> ApiException.notFound("Hospital not found."));
        copy(h, req);
        return Mapper.superHospital(h);
    }

    /** Archive = hide from employees and block its admins; nothing is deleted. */
    @Transactional
    public SuperHospitalDto setHospitalActive(Long id, boolean active) {
        Hospital h = hospitals.findById(id).orElseThrow(() -> ApiException.notFound("Hospital not found."));
        h.setActive(active);
        return Mapper.superHospital(h);
    }

    // ---------- hospital admins ----------
    @Transactional(readOnly = true)
    public List<AdminAccountDto> admins(Long hospitalId) {
        List<HospitalAdmin> rows = hospitalId == null
                ? admins.findAllByOrderByNameAsc()
                : admins.findByHospitalIdOrderByNameAsc(hospitalId);
        return rows.stream().map(Mapper::adminAccount).toList();
    }

    @Transactional
    public AdminAccountDto createAdmin(AdminAccountRequest req) {
        Hospital hospital = hospitals.findById(req.hospitalId())
                .orElseThrow(() -> ApiException.badRequest("Selected hospital does not exist."));
        if (!hospital.isActive()) throw ApiException.badRequest("Cannot create an admin for an archived hospital.");
        if (req.password() == null || req.password().length() < 6) {
            throw ApiException.badRequest("password: must be at least 6 characters.");
        }
        String email = req.email().trim().toLowerCase();
        if (emailTaken(email)) throw ApiException.conflict("This e-mail is already used by another account.");

        HospitalAdmin a = new HospitalAdmin();
        a.setHospital(hospital);
        a.setName(req.name().trim());
        a.setEmail(email);
        a.setPasswordHash(encoder.encode(req.password()));
        a.setActive(true);
        return Mapper.adminAccount(admins.save(a));
    }

    /** Edit name/e-mail/hospital, suspend or reactivate, and optionally reset the password. */
    @Transactional
    public AdminAccountDto updateAdmin(Long id, AdminAccountRequest req) {
        HospitalAdmin a = admins.findById(id).orElseThrow(() -> ApiException.notFound("Administrator not found."));
        Hospital hospital = hospitals.findById(req.hospitalId())
                .orElseThrow(() -> ApiException.badRequest("Selected hospital does not exist."));
        String email = req.email().trim().toLowerCase();
        if (!email.equalsIgnoreCase(a.getEmail()) && emailTaken(email)) {
            throw ApiException.conflict("This e-mail is already used by another account.");
        }
        a.setHospital(hospital);
        a.setName(req.name().trim());
        a.setEmail(email);
        if (req.active() != null) a.setActive(req.active());
        if (req.password() != null && !req.password().isBlank()) a.setPasswordHash(encoder.encode(req.password()));
        return Mapper.adminAccount(a);
    }

    // ---------- helpers ----------
    private boolean emailTaken(String email) {
        return admins.existsByEmailIgnoreCase(email) || superAdmins.existsByEmailIgnoreCase(email)
                || employees.existsByEmailIgnoreCase(email);
    }

    private void validateHours(HospitalRequest req) {
        if (!req.closingTime().isAfter(req.openingTime())) {
            throw ApiException.badRequest("Closing time must be after opening time.");
        }
    }

    private void copy(Hospital h, HospitalRequest req) {
        h.setName(req.name().trim());
        h.setAddress(req.address().trim());
        h.setCity(req.city().trim());
        h.setPhone(req.phone().trim());
        h.setEmail(req.email().trim());
        h.setOpeningTime(req.openingTime());
        h.setClosingTime(req.closingTime());
    }
}
