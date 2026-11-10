package com.motherson.medislot.domain;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalTime;

/** One bookable time window of one doctor on one date. Generated from DoctorSchedule. */
@Entity
@Table(name = "slots",
        uniqueConstraints = @UniqueConstraint(name = "uq_slot", columnNames = {"doctor_id", "slot_date", "start_time"}),
        indexes = @Index(name = "idx_slot_lookup", columnList = "doctor_id, slot_date, booked"))
public class Slot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;
    @Column(name = "slot_date", nullable = false)
    private LocalDate slotDate;
    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;
    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;
    @Column(nullable = false)
    private boolean booked = false;

    public Long getId() { return id; }
    public Doctor getDoctor() { return doctor; }
    public void setDoctor(Doctor doctor) { this.doctor = doctor; }
    public LocalDate getSlotDate() { return slotDate; }
    public void setSlotDate(LocalDate slotDate) { this.slotDate = slotDate; }
    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }
    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }
    public boolean isBooked() { return booked; }
    public void setBooked(boolean booked) { this.booked = booked; }
}
