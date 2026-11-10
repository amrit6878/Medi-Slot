-- Safety net for double booking: at most ONE live (BOOKED/CONFIRMED) appointment per slot.
-- Runs after Hibernate has created the tables (spring.jpa.defer-datasource-initialization=true).
CREATE UNIQUE INDEX IF NOT EXISTS uq_live_appointment_per_slot
    ON appointments (slot_id)
    WHERE status IN ('BOOKED', 'CONFIRMED');
