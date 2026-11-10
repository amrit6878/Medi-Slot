package com.motherson.medislot.repository;

import com.motherson.medislot.domain.SuperAdmin;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SuperAdminRepository extends JpaRepository<SuperAdmin, Long> {
    Optional<SuperAdmin> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);
}
