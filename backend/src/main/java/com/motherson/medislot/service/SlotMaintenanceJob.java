package com.motherson.medislot.service;

import com.motherson.medislot.domain.Doctor;
import com.motherson.medislot.repository.DoctorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Background housekeeping. */
@Component
public class SlotMaintenanceJob {
    private static final Logger log = LoggerFactory.getLogger(SlotMaintenanceJob.class);

    private final DoctorRepository doctors;
    private final SlotService slotService;
    private final AppointmentService appointmentService;

    public SlotMaintenanceJob(DoctorRepository doctors, SlotService slotService, AppointmentService appointmentService) {
        this.doctors = doctors;
        this.slotService = slotService;
        this.appointmentService = appointmentService;
    }

    /** Keeps the rolling booking window full: every night, create slots for the newest day. */
    @Scheduled(cron = "0 5 0 * * *", zone = "${app.timezone}")
    public void extendBookingWindow() {
        int total = 0;
        for (Doctor d : doctors.findByActiveTrue()) {
            try {
                total += slotService.generateFor(d.getId());
            } catch (RuntimeException e) {
                log.warn("Could not generate slots for doctor {}: {}", d.getId(), e.getMessage());
            }
        }
        log.info("Booking window extended: {} new slots", total);
    }

    /** Every 5 minutes: appointments whose time has passed move from Upcoming to Past (COMPLETED). */
    @Scheduled(fixedDelay = 300_000, initialDelay = 30_000)
    public void completeFinishedAppointments() {
        int n = appointmentService.completeFinished();
        if (n > 0) log.info("{} appointment(s) marked COMPLETED", n);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        extendBookingWindow();
        completeFinishedAppointments();
    }
}
