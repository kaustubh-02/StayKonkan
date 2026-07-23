package com.staykonkan.payment.entity;

import com.staykonkan.booking.entity.Booking;
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

/**
 * A single payment attempt/record against a Booking. Money-relevant
 * fields (bookingAmount, commission split, ownerAmount) are captured as
 * a permanent snapshot at payment time — they must NOT be recomputed
 * later from Property/Booking state, since commission percentage or
 * pricing could change after this payment is recorded (audit integrity).
 */
@Entity
@Table(
        name = "payments",
        uniqueConstraints = @UniqueConstraint(name = "uk_payment_gateway_order_id", columnNames = "gateway_order_id")
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Payment extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    // The paying guest — denormalized off booking.guest so payment
    // history/summary queries don't need to join through Booking.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Denormalized off booking.property for the same reason, and
    // because owner-scoped payment history/summary (Module 10A
    // requirement) filters directly on property.owner.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    @Column(name = "gateway_order_id", length = 100)
    private String gatewayOrderId;

    @Column(name = "gateway_payment_id", length = 100)
    private String gatewayPaymentId;

    // Never returned in any API response (see PaymentMapper) — stored
    // only as an audit record of what was verified.
    @Column(name = "gateway_signature", length = 500)
    private String gatewaySignature;

    @Column(name = "booking_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal bookingAmount;

    @Column(name = "platform_commission_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal platformCommissionPercentage;

    @Column(name = "platform_commission_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal platformCommissionAmount;

    @Column(name = "owner_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal ownerAmount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_gateway", nullable = false, length = 30)
    private PaymentGateway paymentGateway;

    // e.g. "card", "upi", "netbanking" — as returned by the gateway;
    // free-form since each gateway's vocabulary differs, and it's
    // display/reporting metadata only, never used in business logic.
    @Column(name = "payment_method", length = 30)
    private String paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 20)
    @Builder.Default
    private PaymentStatus paymentStatus = PaymentStatus.ORDER_CREATED;
}
