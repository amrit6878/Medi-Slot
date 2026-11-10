package com.motherson.medislot.service;

import com.motherson.medislot.domain.Employee;
import com.motherson.medislot.dto.Dtos.EmployeeDto;
import com.motherson.medislot.dto.Dtos.EmployeeUpdateRequest;
import com.motherson.medislot.repository.EmployeeRepository;
import com.motherson.medislot.security.AuthUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmployeeService {
    private final EmployeeRepository employees;
    private final CurrentUserService current;

    public EmployeeService(EmployeeRepository employees, CurrentUserService current) {
        this.employees = employees;
        this.current = current;
    }

    @Transactional(readOnly = true)
    public EmployeeDto me(AuthUser user) {
        return Mapper.employee(current.employee(user));
    }

    /** Employee ID and e-mail come from HR data and cannot be edited here. */
    @Transactional
    public EmployeeDto update(AuthUser user, EmployeeUpdateRequest req) {
        Employee e = employees.findById(current.employee(user).getId()).orElseThrow();
        e.setName(req.name().trim());
        e.setPhone(req.phone() == null || req.phone().isBlank() ? null : req.phone().trim());
        e.setPlantLocation(req.plantLocation() == null || req.plantLocation().isBlank() ? null : req.plantLocation().trim());
        return Mapper.employee(e);
    }
}
