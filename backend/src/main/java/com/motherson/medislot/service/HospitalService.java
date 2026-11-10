package com.motherson.medislot.service;

import com.motherson.medislot.domain.Hospital;
import com.motherson.medislot.dto.Dtos.*;
import com.motherson.medislot.exception.ApiException;
import com.motherson.medislot.repository.DepartmentRepository;
import com.motherson.medislot.repository.DoctorRepository;
import com.motherson.medislot.repository.HospitalRepository;
import com.motherson.medislot.security.AuthUser;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Employee-facing hospital search/details and the hospital admin's own profile. */
@Service
public class HospitalService {
    private final HospitalRepository hospitals;
    private final DoctorRepository doctors;
    private final CurrentUserService current;

    @PersistenceContext
    private EntityManager em;

    public HospitalService(HospitalRepository hospitals, DoctorRepository doctors, CurrentUserService current) {
        this.hospitals = hospitals;
        this.doctors = doctors;
        this.current = current;
    }

    /**
     * @param name free text matched against hospital name, address and city (the search bar)
     * @param city matched against city and address (the location filter)
     */
    @Transactional(readOnly = true)
    public List<HospitalDto> search(String name, String city) {
        Specification<Hospital> spec = (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            p.add(cb.isTrue(root.get("active")));
            if (name != null && !name.isBlank()) {
                String like = "%" + name.trim().toLowerCase() + "%";
                p.add(cb.or(cb.like(cb.lower(root.get("name")), like),
                        cb.like(cb.lower(root.get("address")), like),
                        cb.like(cb.lower(root.get("city")), like)));
            }
            if (city != null && !city.isBlank()) {
                String like = "%" + city.trim().toLowerCase() + "%";
                p.add(cb.or(cb.like(cb.lower(root.get("city")), like),
                        cb.like(cb.lower(root.get("address")), like)));
            }
            return cb.and(p.toArray(new Predicate[0]));
        };
        Map<Long, Long> counts = new HashMap<>();
        for (Object[] row : doctors.activeDoctorCountPerHospital()) counts.put((Long) row[0], (Long) row[1]);

        return hospitals.findAll(spec, Sort.by("name")).stream()
                .map(h -> new HospitalDto(h.getId(), h.getName(), h.getAddress(), h.getCity(), h.getPhone(),
                        h.getEmail(), h.getOpeningTime(), h.getClosingTime(), counts.getOrDefault(h.getId(), 0L)))
                .toList();
    }

    @Transactional(readOnly = true)
    public HospitalDetailDto detail(Long id) {
        Hospital h = hospitals.findById(id).filter(Hospital::isActive)
                .orElseThrow(() -> ApiException.notFound("Hospital not found."));
        List<com.motherson.medislot.domain.Department> deps = em.createQuery("""
                select distinct dep from Doctor d join d.department dep
                where d.hospital.id = :id and d.active = true order by dep.name""",
                com.motherson.medislot.domain.Department.class).setParameter("id", id).getResultList();
        return new HospitalDetailDto(h.getId(), h.getName(), h.getAddress(), h.getCity(), h.getPhone(), h.getEmail(),
                h.getOpeningTime(), h.getClosingTime(), deps.stream().map(Mapper::department).toList());
    }

    // ---- hospital admin: own profile ----

    @Transactional(readOnly = true)
    public SuperHospitalDto myHospital(AuthUser user) {
        Long id = current.adminHospitalId(user);
        return Mapper.superHospital(hospitals.findById(id).orElseThrow());
    }

    @Transactional
    public SuperHospitalDto updateMyHospital(AuthUser user, HospitalRequest req) {
        Long id = current.adminHospitalId(user);
        Hospital h = hospitals.findById(id).orElseThrow();
        if (!req.closingTime().isAfter(req.openingTime())) {
            throw ApiException.badRequest("Closing time must be after opening time.");
        }
        // The partner's name/city are controlled by Motherson; the admin maintains contact details and hours.
        h.setAddress(req.address().trim());
        h.setPhone(req.phone().trim());
        h.setEmail(req.email().trim());
        h.setOpeningTime(req.openingTime());
        h.setClosingTime(req.closingTime());
        return Mapper.superHospital(h);
    }
}
