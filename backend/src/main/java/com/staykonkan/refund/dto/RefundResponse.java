package com.staykonkan.refund.dto;

import com.staykonkan.refund.entity.RefundStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@Builder
public class RefundResponse {

    private Long id;

    private String refundReference;

    private Long paymentId;

    private Long bookingId;

    private String bookingCode;

    private Long userId;

    private String razorpayRefundId;

    private BigDecimal refundAmount;

    private String refundReason;

    private RefundStatus status;

    private Instant requestedAt;

    private Instant processedAt;

    private Instant createdAt;

    private Instant updatedAt;
}
