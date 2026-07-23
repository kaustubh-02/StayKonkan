package com.staykonkan.payment.dto;

import com.staykonkan.payment.entity.PaymentGateway;
import com.staykonkan.payment.entity.PaymentStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

/** gatewaySignature is deliberately never exposed here — see PaymentMapper. */
@Getter
@Setter
@Builder
public class PaymentResponse {

    private Long id;

    private Long bookingId;

    private String bookingCode;

    private Long userId;

    private Long propertyId;

    private String propertyTitle;

    private String gatewayOrderId;

    private String gatewayPaymentId;

    private BigDecimal bookingAmount;

    private BigDecimal platformCommissionPercentage;

    private BigDecimal platformCommissionAmount;

    private BigDecimal ownerAmount;

    private String currency;

    private PaymentGateway paymentGateway;

    private String paymentMethod;

    private PaymentStatus paymentStatus;

    private Instant createdAt;

    private Instant updatedAt;
}
