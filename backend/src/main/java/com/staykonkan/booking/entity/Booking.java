package com.staykonkan.booking.entity;

import com.staykonkan.entity.AuditableEntity;
import com.staykonkan.property.entity.Property;
import com.staykonkan.user.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "bookings",
        indexes = {
                @Index(name = "idx_booking_guest", columnList = "guest_id"),
                @Index(name = "idx_booking_property", columnList = "property_id"),
                @Index(name = "idx_booking_status", columnList = "status"),
                @Index(name = "idx_booking_dates", columnList = "check_in_date,check_out_date")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Booking extends AuditableEntity {

    @Column(nullable = false, unique = true, length = 30)
    private String bookingCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guest_id", nullable = false)
    private User guest;

    @Column(nullable = false)
    private LocalDate checkInDate;

    @Column(nullable = false)
    private LocalDate checkOutDate;

    @Column(nullable = false)
    private Integer guestCount;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal pricePerNight;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private BookingStatus status = BookingStatus.PENDING;

    @Column(length = 1000)
    private String specialRequests;

    private LocalDateTime confirmedAt;

    private LocalDateTime cancelledAt;

    private LocalDateTime completedAt;
}