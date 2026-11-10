package com.motherson.medislot.service;

import com.motherson.medislot.domain.Employee;
import com.motherson.medislot.domain.HospitalAdmin;
import com.motherson.medislot.domain.SuperAdmin;
import com.motherson.medislot.dto.Dtos.LoginRequest;
import com.motherson.medislot.dto.Dtos.LoginResponse;
import com.motherson.medislot.dto.Dtos.UserInfo;
import com.motherson.medislot.exception.ApiException;
import com.motherson.medislot.repository.EmployeeRepository;
import com.motherson.medislot.repository.HospitalAdminRepository;
import com.motherson.medislot.repository.SuperAdminRepository;
import com.motherson.medislot.security.AuthUser;
import com.motherson.medislot.security.JwtService;
import com.motherson.medislot.security.Role;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * One login form for everyone:
 *  - "EMP001"            -> employee by Employee ID
 *  - "name@company.com"  -> employee, hospital admin or super admin by e-mail
 */
@Service
public class AuthService {
    private static final String INVALID = "Invalid Employee ID / email or password.";

    private final EmployeeRepository employees;
    private final HospitalAdminRepository admins;
    private final SuperAdminRepository superAdmins;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    public AuthService(EmployeeRepository employees, HospitalAdminRepository admins,
                       SuperAdminRepository superAdmins, PasswordEncoder encoder, JwtService jwt) {
        this.employees = employees;
        this.admins = admins;
        this.superAdmins = superAdmins;
        this.encoder = encoder;
        this.jwt = jwt;
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest req) {
        String identifier = req.identifier().trim();
        String password = req.password();
        boolean isEmail = identifier.contains("@");

        Optional<Employee> employee = isEmail
                ? employees.findByEmailIgnoreCase(identifier)
                : employees.findByEmployeeCodeIgnoreCase(identifier);
        if (employee.isPresent() && encoder.matches(password, employee.get().getPasswordHash())) {
            Employee e = employee.get();
            if (!e.isActive()) throw ApiException.forbidden("This account is inactive. Please contact your HR team.");
            return respond(new AuthUser(e.getId(), Role.EMPLOYEE, null, e.getEmail()),
                    new UserInfo(e.getId(), e.getName(), e.getEmail(), e.getEmployeeCode(), null, null, "EMPLOYEE"));
        }

        if (isEmail) {
            Optional<HospitalAdmin> admin = admins.findByEmailIgnoreCase(identifier);
            if (admin.isPresent() && encoder.matches(password, admin.get().getPasswordHash())) {
                HospitalAdmin a = admin.get();
                if (!a.isActive()) throw ApiException.forbidden("This administrator account is suspended.");
                if (!a.getHospital().isActive()) throw ApiException.forbidden("This hospital is no longer a partner.");
                return respond(new AuthUser(a.getId(), Role.ADMIN, a.getHospital().getId(), a.getEmail()),
                        new UserInfo(a.getId(), a.getName(), a.getEmail(), null,
                                a.getHospital().getId(), a.getHospital().getName(), "ADMIN"));
            }

            Optional<SuperAdmin> superAdmin = superAdmins.findByEmailIgnoreCase(identifier);
            if (superAdmin.isPresent() && encoder.matches(password, superAdmin.get().getPasswordHash())) {
                SuperAdmin s = superAdmin.get();
                if (!s.isActive()) throw ApiException.forbidden("This account is disabled.");
                return respond(new AuthUser(s.getId(), Role.SUPER_ADMIN, null, s.getEmail()),
                        new UserInfo(s.getId(), s.getName(), s.getEmail(), null, null, null, "SUPER_ADMIN"));
            }
        }
        throw ApiException.unauthorized(INVALID);
    }

    private LoginResponse respond(AuthUser principal, UserInfo info) {
        return new LoginResponse(jwt.generate(principal), principal.role().name(), jwt.expiresInSeconds(), info);
    }
}
