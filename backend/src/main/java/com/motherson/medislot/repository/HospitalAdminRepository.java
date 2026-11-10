package com.motherson.medislot.repository;

import com.motherson.medislot.domain.HospitalAdmin;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface HospitalAdminRepository extends JpaRepository<HospitalAdmin, Long> {
    @EntityGraph(attributePaths = "hospital")
    Optional<HospitalAdmin> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    @EntityGraph(attributePaths = "hospital")
    List<HospitalAdmin> findAllByOrderByNameAsc();

    @EntityGraph(attributePaths = "hospital")
    List<HospitalAdmin> findByHospitalIdOrderByNameAsc(Long hospitalId);
}
