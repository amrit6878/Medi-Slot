package com.motherson.medislot.repository;

import com.motherson.medislot.domain.Doctor;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DoctorRepository extends JpaRepository<Doctor, Long>, JpaSpecificationExecutor<Doctor> {

    @Override
    @EntityGraph(attributePaths = {"department", "hospital"})
    List<Doctor> findAll(org.springframework.data.jpa.domain.Specification<Doctor> spec,
                         org.springframework.data.domain.Sort sort);

    @EntityGraph(attributePaths = {"department", "hospital"})
    @Query("select d from Doctor d where d.id = :id")
    Optional<Doctor> findDetailedById(@Param("id") Long id);

    @EntityGraph(attributePaths = {"department", "hospital"})
    List<Doctor> findByHospitalIdOrderByNameAsc(Long hospitalId);

    @EntityGraph(attributePaths = {"department", "hospital"})
    Optional<Doctor> findByIdAndHospitalId(Long id, Long hospitalId);

    List<Doctor> findByActiveTrue();

    @Query("select d.hospital.id, count(d) from Doctor d where d.active = true group by d.hospital.id")
    List<Object[]> activeDoctorCountPerHospital();

    @Query("select count(d) from Doctor d where d.hospital.id = :hospitalId")
    long countByHospital(@Param("hospitalId") Long hospitalId);
}
