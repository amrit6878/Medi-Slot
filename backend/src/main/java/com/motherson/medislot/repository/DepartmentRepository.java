package com.motherson.medislot.repository;

import com.motherson.medislot.domain.Department;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DepartmentRepository extends JpaRepository<Department, Long> {
    List<Department> findAllByOrderByNameAsc();
    Optional<Department> findByNameIgnoreCase(String name);
}
