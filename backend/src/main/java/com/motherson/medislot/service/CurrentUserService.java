package com.motherson.medislot.service;

import com.motherson.medislot.domain.Employee;
import com.motherson.medislot.domain.HospitalAdmin;
import com.motherson.medislot.exception.ApiException;
import com.motherson.medislot.repository.EmployeeRepository;
import com.motherson.medislot.repository.HospitalAdminRepository;
import com.motherson.medislot.security.AuthUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Re-checks the account on each request so suspending someone takes effect immediately. */
@Service
public class CurrentUserService {
    private final EmployeeRepository employees;
    private final HospitalAdminRepository admins;

    public CurrentUserService(EmployeeRepository employees, HospitalAdminRepository admins) {
        this.employees = employees;
        this.admins = admins;
    }

    @Transactional(readOnly = true)
    public Employee employee(AuthUser user) {
        Employee e = employees.findById(user.id())
                .orElseThrow(() -> ApiException.unauthorized("Account not found. Please log in again."));
        if (!e.isActive()) throw ApiException.forbidden("This account is inactive.");
        return e;
    }

    /** @return the id of the hospital the logged-in admin manages. */
    @Transactional(readOnly = true)
    public Long adminHospitalId(AuthUser user) {
        HospitalAdmin a = admins.findById(user.id())
                .orElseThrow(() -> ApiException.unauthorized("Account not found. Please log in again."));
        if (!a.isActive()) throw ApiException.forbidden("This administrator account is suspended.");
        if (!a.getHospital().isActive()) throw ApiException.forbidden("This hospital has been archived.");
        return a.getHospital().getId();
    }

    @Transactional(readOnly = true)
    public String hospitalName(AuthUser user) {
        return admins.findById(user.id()).map(a -> a.getHospital().getName()).orElse(null);
    }
}
