package com.motherson.medislot.domain;

import java.util.EnumSet;
import java.util.Set;

public enum AppointmentStatus {
    BOOKED, CONFIRMED, CANCELLED, COMPLETED, NO_SHOW;

    /** Statuses that still occupy a slot. */
    public static final Set<AppointmentStatus> LIVE = EnumSet.of(BOOKED, CONFIRMED);
}
