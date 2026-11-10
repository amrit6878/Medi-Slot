package com.motherson.medislot.controller;

import com.motherson.medislot.dto.Dtos.*;
import com.motherson.medislot.service.SuperAdminService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Motherson super admin (role SUPER_ADMIN): partner hospitals and hospital-admin credentials. */
@RestController
@RequestMapping("/super-admin")
public class SuperAdminController {
    private final SuperAdminService service;

    public SuperAdminController(SuperAdminService service) { this.service = service; }

    @GetMapping("/hospitals/dashboard")
    public SuperDashboardDto dashboard() { return service.dashboard(); }

    @GetMapping("/hospitals")
    public List<SuperHospitalDto> hospitals() { return service.hospitals(); }

    @PostMapping("/hospitals")
    public SuperHospitalDto createHospital(@Valid @RequestBody HospitalRequest request) {
        return service.createHospital(request);
    }

    @PutMapping("/hospitals/{id}")
    public SuperHospitalDto updateHospital(@PathVariable Long id, @Valid @RequestBody HospitalRequest request) {
        return service.updateHospital(id, request);
    }

    /** "Delete" archives the hospital (soft delete). */
    @DeleteMapping("/hospitals/{id}")
    public SuperHospitalDto archiveHospital(@PathVariable Long id) {
        return service.setHospitalActive(id, false);
    }

    @PutMapping("/hospitals/{id}/restore")
    public SuperHospitalDto restoreHospital(@PathVariable Long id) {
        return service.setHospitalActive(id, true);
    }

    @GetMapping("/admins")
    public List<AdminAccountDto> admins(@RequestParam(required = false) Long hospitalId) {
        return service.admins(hospitalId);
    }

    @PostMapping("/admins")
    public AdminAccountDto createAdmin(@Valid @RequestBody AdminAccountRequest request) {
        return service.createAdmin(request);
    }

    @PutMapping("/admins/{id}")
    public AdminAccountDto updateAdmin(@PathVariable Long id, @Valid @RequestBody AdminAccountRequest request) {
        return service.updateAdmin(id, request);
    }
}
