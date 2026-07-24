package com.staykonkan.settlement.entity;

import com.staykonkan.booking.entity.Booking;
import com.staykonkan.entity.AuditableEntity;
import com.staykonkan.payment.entity.Payment;
import com.staykonkan.property.entity.Property;
import com.staykonkan.user.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Tracks payout of one Payment's already-computed ownerAmount to the
 * property owner. Money-relevant fields (bookingAmount, commission
 * split, settlementAmount) are snapshotted from Payment at settlement-
 * creation time — NOT recomputed here. Payment (Module 10A) is the
 * single source of truth for commission math; this entity's job is
 * tracking the payout lifecycle of an amount Payment already calculated,
 * same "never duplicate payment logic" principle Module 10C followed
 * for refunds.
 * <p>
 * No external payout gateway integration in this module (no RazorpayX/
 * Route call) — settlement status is advanced by platform ops via the
 * API (e.g. after an off-platform bank transfer), consistent with what
 * was actually requested. gatewayTransferId is included as a nullable,
 * optional field so a real payout-gateway integration can populate it
 * later without a schema change.
 */
@Entity
@Table(name = "settlements")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Settlement extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    // The payout recipient — denormalized off property.owner, same
    // reasoning Payment already uses for its own user/property fields:
    // owner-scoped settlement history/summary queries filter directly on
    // this column without joining through Property.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(name = "settlement_reference", nullable = false, unique = true, length = 40)
    private String settlementReference;

    @Column(name = "booking_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal bookingAmount;

    @Column(name = "platform_commission_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal platformCommissionPercentage;

    @Column(name = "platform_commission_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal platformCommissionAmount;

    // The actual payout amount — equal to Payment.ownerAmount at the
    // time this Settlement was created.
    @Column(name = "settlement_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal settlementAmount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private SettlementStatus status = SettlementStatus.PENDING;

    // Reserved for a future real payout-gateway integration; unused by
    // this module's own logic.
    @Column(name = "gateway_transfer_id", length = 100)
    private String gatewayTransferId;

    // Free-text ops note, e.g. "Paid via NEFT on 2026-07-25, ref #1234".
    @Column(length = 500)
    private String notes;

    @Column(name = "initiated_at")
    private Instant initiatedAt;

    @Column(name = "completed_at")
    private Instant completedAt;
}
