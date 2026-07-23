package com.staykonkan.refund.entity;

import com.staykonkan.booking.entity.Booking;
import com.staykonkan.entity.AuditableEntity;
import com.staykonkan.payment.entity.Payment;
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
 * One refund attempt against a Payment. Note on the spec's field list:
 * "refundId" is this row's own primary key — inherited from BaseEntity
 * as {@code id}, not redeclared here — and "createdBy"/"updatedAt" are
 * already provided by AuditableEntity's JPA-auditing columns, same as
 * every other entity in this codebase. Both are satisfied by
 * inheritance rather than duplicated as separate fields.
 * <p>
 * No unique constraint on payment_id: a FAILED refund attempt must be
 * retriable (e.g. a transient gateway error), so "prevent duplicate
 * refunds" is enforced at the application level in RefundServiceImpl
 * (block a new request only while an existing refund for the same
 * payment is REQUESTED, PROCESSING, or already SUCCESS) rather than by
 * a blanket DB constraint — same reasoning Payment itself already uses
 * for duplicate-order prevention (Module 10A).
 */
@Entity
@Table(name = "refunds")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Refund extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    // The requester — the booking's guest in the normal flow, or an
    // admin acting on the guest's behalf (see RefundServiceImpl).
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "refund_reference", nullable = false, unique = true, length = 40)
    private String refundReference;

    @Column(name = "razorpay_refund_id", length = 100)
    private String razorpayRefundId;

    @Column(name = "refund_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal refundAmount;

    @Column(name = "refund_reason", length = 500)
    private String refundReason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private RefundStatus status = RefundStatus.REQUESTED;

    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;

    @Column(name = "processed_at")
    private Instant processedAt;
}
