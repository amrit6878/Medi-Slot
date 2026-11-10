package com.motherson.medislot.config;

import com.motherson.medislot.domain.*;
import com.motherson.medislot.repository.*;
import com.motherson.medislot.service.SlotService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalTime;
import java.util.List;

/**
 * Start-up data.
 *  - ALWAYS: department list + one super admin (so a fresh database is usable).
 *  - Only when app.seed-demo-data=true and the tables are empty: demo hospitals, hospital admins,
 *    doctors (Mon-Sat 09:00-17:00, 30 min) and employees. Every demo password is "password123".
 */
@Component
public class DataSeeder implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);
    private static final String DEMO_PASSWORD = "password123";

    private final AppProperties props;
    private final PasswordEncoder encoder;
    private final TransactionTemplate tx;
    private final DepartmentRepository departments;
    private final SuperAdminRepository superAdmins;
    private final HospitalRepository hospitals;
    private final HospitalAdminRepository admins;
    private final DoctorRepository doctors;
    private final DoctorScheduleRepository schedules;
    private final EmployeeRepository employees;
    private final SlotService slotService;

    public DataSeeder(AppProperties props, PasswordEncoder encoder, TransactionTemplate tx,
                      DepartmentRepository departments, SuperAdminRepository superAdmins,
                      HospitalRepository hospitals, HospitalAdminRepository admins, DoctorRepository doctors,
                      DoctorScheduleRepository schedules, EmployeeRepository employees, SlotService slotService) {
        this.props = props;
        this.encoder = encoder;
        this.tx = tx;
        this.departments = departments;
        this.superAdmins = superAdmins;
        this.hospitals = hospitals;
        this.admins = admins;
        this.doctors = doctors;
        this.schedules = schedules;
        this.employees = employees;
        this.slotService = slotService;
    }

    @Override
    public void run(String... args) {
        tx.executeWithoutResult(s -> {
            seedDepartments();
            seedSuperAdmin();
            if (props.seedDemoData() && hospitals.count() == 0) seedDemo();
        });
        // slots need the committed doctors + schedules
        if (props.seedDemoData()) {
            doctors.findByActiveTrue().forEach(d -> slotService.generateFor(d.getId()));
        }
    }

    private void seedDepartments() {
        for (String name : List.of("Cardiology", "General Medicine", "Orthopedics", "Dermatology", "Pediatrics",
                "Neurology", "Gynecology", "ENT", "Ophthalmology", "Dentistry", "Psychiatry", "Urology")) {
            if (departments.findByNameIgnoreCase(name).isEmpty()) departments.save(new Department(name));
        }
    }

    private void seedSuperAdmin() {
        if (superAdmins.count() > 0) return;
        var b = props.bootstrap();
        SuperAdmin sa = new SuperAdmin();
        sa.setName(b.superAdminName());
        sa.setEmail(b.superAdminEmail().toLowerCase());
        sa.setPasswordHash(encoder.encode(b.superAdminPassword()));
        superAdmins.save(sa);
        log.info("Created super admin {}", sa.getEmail());
    }

    private void seedDemo() {
        Hospital cityCare = hospital("City Care Hospital", "Sector 62", "Noida", "0120-4001000", "info@citycare.example");
        Hospital medLife = hospital("MedLife Hospital", "Sector 18", "Noida", "0120-4002000", "info@medlife.example");
        Hospital healthPlus = hospital("HealthPlus Hospital", "Sector 135", "Noida", "0120-4003000", "info@healthplus.example");
        Hospital aarogya = hospital("Aarogya Super Specialty", "Sector 50", "Gurugram", "0124-4004000", "info@aarogya.example");
        Hospital sunrise = hospital("Sunrise Medical Centre", "Golf Course Road", "Gurugram", "0124-4005000", "info@sunrise.example");
        Hospital wellSpring = hospital("WellSpring Hospital", "Electronic City", "Bengaluru", "080-4006000", "info@wellspring.example");

        admin(cityCare, "City Care Admin", "admin.noida@medislot.com");
        admin(medLife, "MedLife Admin", "admin.medlife@medislot.com");
        admin(healthPlus, "HealthPlus Admin", "admin.healthplus@medislot.com");
        admin(aarogya, "Aarogya Admin", "admin.aarogya@medislot.com");
        admin(sunrise, "Sunrise Admin", "admin.sunrise@medislot.com");
        admin(wellSpring, "WellSpring Admin", "admin.wellspring@medislot.com");

        doctor(cityCare, "Cardiology", "Dr. Rohan Mehta", "Cardiologist", "MBBS, MD (Cardiology)", 10);
        doctor(cityCare, "Orthopedics", "Dr. Amit Verma", "Orthopedic Surgeon", "MBBS, MS (Orthopedics)", 12);
        doctor(cityCare, "Pediatrics", "Dr. Kavita Iyer", "Pediatrician", "MBBS, MD (Pediatrics)", 11);
        doctor(cityCare, "ENT", "Dr. Arjun Malhotra", "ENT Specialist", "MBBS, MS (ENT)", 10);
        doctor(medLife, "General Medicine", "Dr. Priya Sharma", "General Physician", "MBBS, MD (Medicine)", 8);
        doctor(medLife, "Neurology", "Dr. Sandeep Rao", "Neurologist", "MBBS, DM (Neurology)", 15);
        doctor(healthPlus, "Dermatology", "Dr. Neha Gupta", "Dermatologist", "MBBS, MD (Dermatology)", 9);
        doctor(healthPlus, "Gynecology", "Dr. Farah Khan", "Gynecologist", "MBBS, MS (Obstetrics & Gynaecology)", 13);
        doctor(aarogya, "General Medicine", "Dr. Vikram Singh", "General Physician", "MBBS, MD (Medicine)", 14);
        doctor(aarogya, "Cardiology", "Dr. Anjali Desai", "Cardiologist", "MBBS, DM (Cardiology)", 16);
        doctor(sunrise, "Orthopedics", "Dr. Rahul Kapoor", "Orthopedic Surgeon", "MBBS, MS (Orthopedics)", 11);
        doctor(sunrise, "Dermatology", "Dr. Meera Nair", "Dermatologist", "MBBS, MD (Dermatology)", 7);
        doctor(wellSpring, "General Medicine", "Dr. Suresh Reddy", "General Physician", "MBBS, MD (Medicine)", 12);
        doctor(wellSpring, "Pediatrics", "Dr. Lakshmi Iyer", "Pediatrician", "MBBS, DCH", 9);

        employee("EMP001", "Rahul Sharma", "rahul.sharma@motherson.com", "9876543210", "Noida");
        employee("EMP002", "Anita Verma", "anita.verma@motherson.com", "9876543211", "Gurugram");
        employee("EMP003", "Karthik Rajan", "karthik.rajan@motherson.com", "9876543212", "Bengaluru");
        log.info("Demo data created (employee EMP001 / admin.noida@medislot.com, password '{}')", DEMO_PASSWORD);
    }

    private Hospital hospital(String name, String address, String city, String phone, String email) {
        Hospital h = new Hospital();
        h.setName(name);
        h.setAddress(address);
        h.setCity(city);
        h.setPhone(phone);
        h.setEmail(email);
        return hospitals.save(h);
    }

    private void admin(Hospital h, String name, String email) {
        HospitalAdmin a = new HospitalAdmin();
        a.setHospital(h);
        a.setName(name);
        a.setEmail(email);
        a.setPasswordHash(encoder.encode(DEMO_PASSWORD));
        admins.save(a);
    }

    private void doctor(Hospital h, String dept, String name, String specialization, String qualification, int years) {
        Doctor d = new Doctor();
        d.setHospital(h);
        d.setDepartment(departments.findByNameIgnoreCase(dept).orElseThrow());
        d.setName(name);
        d.setSpecialization(specialization);
        d.setQualification(qualification);
        d.setExperienceYears(years);
        d.setAbout(name + " is an experienced " + specialization.toLowerCase() + " at " + h.getName() + ".");
        doctors.save(d);
        for (int day = 1; day <= 6; day++) {          // Monday - Saturday
            DoctorSchedule s = new DoctorSchedule();
            s.setDoctor(d);
            s.setDayOfWeek(day);
            s.setStartTime(LocalTime.of(9, 0));
            s.setEndTime(LocalTime.of(17, 0));
            s.setSlotDurationMinutes(30);
            schedules.save(s);
        }
    }

    private void employee(String code, String name, String email, String phone, String plant) {
        Employee e = new Employee();
        e.setEmployeeCode(code);
        e.setName(name);
        e.setEmail(email);
        e.setPhone(phone);
        e.setPlantLocation(plant);
        e.setPasswordHash(encoder.encode(DEMO_PASSWORD));
        employees.save(e);
    }
}
