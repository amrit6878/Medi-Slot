package com.motherson.medislot.repository;

import com.motherson.medislot.domain.DoctorSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface DoctorScheduleRepository extends JpaRepository<DoctorSchedule, Long> {
    List<DoctorSchedule> findByDoctorIdOrderByDayOfWeekAsc(Long doctorId);

    @Query("select s from DoctorSchedule s where s.doctor.id in :ids order by s.dayOfWeek")
    List<DoctorSchedule> findByDoctorIds(@Param("ids") Collection<Long> ids);

    /** Bulk delete runs immediately (needed so re-inserting the same weekdays does not violate the unique key). */
    @Modifying(flushAutomatically = true)
    @Query("delete from DoctorSchedule s where s.doctor.id = :doctorId")
    void deleteAllForDoctor(@Param("doctorId") Long doctorId);
}
