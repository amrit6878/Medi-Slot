package com.motherson.medislot.service;

import com.motherson.medislot.domain.HospitalAdmin;
import com.motherson.medislot.dto.Dtos.AdminSettingsDto;
import com.motherson.medislot.exception.ApiException;
import com.motherson.medislot.repository.HospitalAdminRepository;
import com.motherson.medislot.security.AuthUser;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Self-service settings for the logged-in hospital admin (profile name + password). */
@Service
public class AdminAccountService {
    private final HospitalAdminRepository admins;
    private final PasswordEncoder encoder;

    public AdminAccountService(HospitalAdminRepository admins, PasswordEncoder encoder) {
        this.admins = admins;
        this.encoder = encoder;
    }

    @Transactional(readOnly = true)
    public AdminSettingsDto get(AuthUser user) {
        return toDto(load(user));
    }

    @Transactional
    public AdminSettingsDto updateName(AuthUser user, String name) {
        HospitalAdmin admin = load(user);
        admin.setName(name.trim());
        return toDto(admin);
    }

    @Transactional
    public void changePassword(AuthUser user, String currentPassword, String newPassword) {
        HospitalAdmin admin = load(user);
        if (!encoder.matches(currentPassword, admin.getPasswordHash())) {
            throw ApiException.badRequest("Your current password is incorrect.");
        }
        if (encoder.matches(newPassword, admin.getPasswordHash())) {
            throw ApiException.badRequest("The new password must be different from the current one.");
        }
        admin.setPasswordHash(encoder.encode(newPassword));
    }

    private HospitalAdmin load(AuthUser user) {
        HospitalAdmin admin = admins.findById(user.id())
                .orElseThrow(() -> ApiException.unauthorized("Account not found. Please log in again."));
        if (!admin.isActive()) throw ApiException.forbidden("This administrator account is suspended.");
        return admin;
    }

    private AdminSettingsDto toDto(HospitalAdmin a) {
        return new AdminSettingsDto(a.getName(), a.getEmail(), a.getHospital().getName());
    }
}
