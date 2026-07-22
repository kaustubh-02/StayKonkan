package com.staykonkan.availability.entity;

import com.staykonkan.booking.entity.Booking;
import com.staykonkan.entity.AuditableEntity;
import com.staykonkan.property.entity.Property;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * One row per (property, calendar date) that is NOT in its implicit
 * default AVAILABLE state. Storage is deliberately sparse: a date with no
 * row here is available. This keeps the table bounded by real booking /
 * blocking activity instead of growing with every property's entire
 * future calendar, while the unique constraint below is also the last
 * line of defense against a double-booking race condition (see
 * PropertyAvailabilityServiceImpl.bookDates).
 */
@Entity
@Table(
        name = "property_availability",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_property_availability_date",
                columnNames = {"property_id", "available_date"}
        )
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PropertyAvailability extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    @Column(name = "available_date", nullable = false)
    private LocalDate availableDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private AvailabilityStatus status = AvailabilityStatus.AVAILABLE;

    // Set only when status = BOOKED; null for BLOCKED/MAINTENANCE rows
    // (which are host-managed, not tied to any specific booking).
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id")
    private Booking booking;
}
