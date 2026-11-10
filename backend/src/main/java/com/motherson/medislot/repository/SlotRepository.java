package com.motherson.medislot.repository;

import com.motherson.medislot.domain.Slot;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface SlotRepository extends JpaRepository<Slot, Long> {

    /** Row lock so two employees cannot book the same slot at the same moment. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Slot s where s.id = :id")
    Optional<Slot> findByIdForUpdate(@Param("id") Long id);

    @Query("""
            select s from Slot s
            where s.doctor.id = :doctorId and s.slotDate between :from and :to
            order by s.slotDate, s.startTime""")
    List<Slot> findInRange(@Param("doctorId") Long doctorId,
                           @Param("from") LocalDate from,
                           @Param("to") LocalDate to);

    @Query("""
            select s from Slot s
            where s.doctor.id = :doctorId and s.slotDate = :date and s.booked = false
              and (s.slotDate > :today or s.startTime > :now)
            order by s.startTime""")
    List<Slot> findOpenSlots(@Param("doctorId") Long doctorId,
                             @Param("date") LocalDate date,
                             @Param("today") LocalDate today,
                             @Param("now") LocalTime now);

    @Query("""
            select distinct s.doctor.id from Slot s
            where s.slotDate = :date and s.booked = false
              and (s.slotDate > :today or s.startTime > :now)""")
    List<Long> doctorIdsWithOpenSlots(@Param("date") LocalDate date,
                                      @Param("today") LocalDate today,
                                      @Param("now") LocalTime now);

    /** For each given doctor: the earliest date (on/after :from) that still has a free, not-yet-started slot. */
    @Query("""
            select s.doctor.id, min(s.slotDate) from Slot s
            where s.doctor.id in :doctorIds and s.booked = false and s.slotDate >= :from
              and (s.slotDate > :today or s.startTime > :now)
            group by s.doctor.id""")
    List<Object[]> nextAvailableDates(@Param("doctorIds") java.util.Collection<Long> doctorIds,
                                      @Param("from") LocalDate from,
                                      @Param("today") LocalDate today,
                                      @Param("now") LocalTime now);

    /** Removes slots nobody has booked yet (used when a doctor's weekly schedule is replaced). */
    @Modifying(flushAutomatically = true)
    @Query("delete from Slot s where s.doctor.id = :doctorId and s.booked = false and s.slotDate >= :from")
    int deleteOpenSlotsFrom(@Param("doctorId") Long doctorId, @Param("from") LocalDate from);

    @Query("""
            select count(distinct s.doctor.id) from Slot s
            where s.doctor.hospital.id = :hospitalId and s.doctor.active = true and s.slotDate = :date""")
    long countDoctorsWithSlotsOn(@Param("hospitalId") Long hospitalId, @Param("date") LocalDate date);
}
