package com.staykonkan.availability.entity;

/** Per-date status of a property. AVAILABLE is the implicit default for any date with no row (see repository/service Javadoc for the sparse-storage rationale). */
public enum AvailabilityStatus {
    AVAILABLE,
    BOOKED,
    BLOCKED,
    MAINTENANCE
}
