package com.motherson.medislot.repository;

import com.motherson.medislot.domain.Appointment;
import com.motherson.medislot.domain.AppointmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AppointmentRepository extends JpaRepository<Appointment, Long>, JpaSpecificationExecutor<Appointment> {

    /** Report source: appointment counts per day / doctor / status for one hospital. */
    @Query("""
            select s.slotDate, d.id, d.name, dep.name, a.status, count(a)
            from Appointment a join a.slot s join a.doctor d join d.department dep
            where a.hospital.id = :hospitalId and s.slotDate between :from and :to
            group by s.slotDate, d.id, d.name, dep.name, a.status""")
    List<Object[]> reportRows(@Param("hospitalId") Long hospitalId,
                              @Param("from") LocalDate from,
                              @Param("to") LocalDate to);

    String FETCH = " join fetch a.slot s join fetch a.doctor d join fetch d.department "
            + "join fetch a.hospital join fetch a.employee ";

    @Override
    @EntityGraph(attributePaths = {"slot", "doctor", "doctor.department", "hospital", "employee"})
    Page<Appointment> findAll(Specification<Appointment> spec, Pageable pageable);

    @Query("select a from Appointment a " + FETCH + " where a.id = :id")
    Optional<Appointment> findDetailedById(@Param("id") Long id);

    /** Still live and the slot has not ended yet. */
    @Query("select a from Appointment a " + FETCH + """
             where a.employee.id = :employeeId and a.status in :live
               and (s.slotDate > :today or (s.slotDate = :today and s.endTime > :now))
             order by s.slotDate asc, s.startTime asc""")
    List<Appointment> findUpcoming(@Param("employeeId") Long employeeId,
                                   @Param("live") Collection<AppointmentStatus> live,
                                   @Param("today") LocalDate today,
                                   @Param("now") LocalTime now);

    /** Completed / cancelled / no-show, or live but the slot is already over. */
    @Query("select a from Appointment a " + FETCH + """
             where a.employee.id = :employeeId
               and (a.status not in :live
                    or s.slotDate < :today or (s.slotDate = :today and s.endTime <= :now))
             order by s.slotDate desc, s.startTime desc""")
    List<Appointment> findPast(@Param("employeeId") Long employeeId,
                               @Param("live") Collection<AppointmentStatus> live,
                               @Param("today") LocalDate today,
                               @Param("now") LocalTime now);

    @Query("""
            select count(a) from Appointment a
            where a.employee.id = :employeeId and a.status in :live
              and a.slot.slotDate = :date and a.slot.startTime < :end and a.slot.endTime > :start""")
    long countOverlapping(@Param("employeeId") Long employeeId,
                          @Param("live") Collection<AppointmentStatus> live,
                          @Param("date") LocalDate date,
                          @Param("start") LocalTime start,
                          @Param("end") LocalTime end);

    /** Moves every live appointment whose slot is over to COMPLETED. */
    @Modifying(flushAutomatically = true)
    @Query("""
            update Appointment a set a.status = :done, a.updatedAt = :now
            where a.status in :live and a.slot.id in (
                select s.id from Slot s
                where s.slotDate < :today or (s.slotDate = :today and s.endTime <= :time))""")
    int completeFinished(@Param("done") AppointmentStatus done,
                         @Param("live") Collection<AppointmentStatus> live,
                         @Param("now") Instant now,
                         @Param("today") LocalDate today,
                         @Param("time") LocalTime time);

    @Query("""
            select count(a) from Appointment a
            where a.hospital.id = :hospitalId and a.slot.slotDate = :date and a.status <> :excluded""")
    long countOnDateExcluding(@Param("hospitalId") Long hospitalId,
                              @Param("date") LocalDate date,
                              @Param("excluded") AppointmentStatus excluded);

    @Query("""
            select count(a) from Appointment a
            where a.hospital.id = :hospitalId and a.status = :status""")
    long countByHospitalAndStatus(@Param("hospitalId") Long hospitalId,
                                  @Param("status") AppointmentStatus status);

    @Query("""
            select count(a) from Appointment a
            where a.hospital.id = :hospitalId and a.slot.slotDate = :date and a.status = :status""")
    long countOnDateWithStatus(@Param("hospitalId") Long hospitalId,
                               @Param("date") LocalDate date,
                               @Param("status") AppointmentStatus status);
}
