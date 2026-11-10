package com.motherson.medislot.repository;

import com.motherson.medislot.domain.Hospital;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface HospitalRepository extends JpaRepository<Hospital, Long>, JpaSpecificationExecutor<Hospital> {
    List<Hospital> findAllByOrderByNameAsc();
    long countByActiveTrue();
    boolean existsByNameIgnoreCaseAndCityIgnoreCase(String name, String city);
}
