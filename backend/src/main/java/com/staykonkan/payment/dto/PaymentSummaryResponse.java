package com.staykonkan.payment.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Scope adapts to the caller's role (enforced in PaymentServiceImpl,
 * same pattern as getPaymentHistory): a USER sees their own spend, an
 * OWNER sees earnings across their properties, an ADMIN sees the
 * platform-wide picture. successfulPaymentCount only counts SUCCESS
 * payments — totals are meaningless to aggregate across FAILED/CANCELLED
 * attempts.
 */
@Getter
@Setter
@Builder
public class PaymentSummaryResponse {

    private long successfulPaymentCount;

    private BigDecimal totalBookingAmount;

    private BigDecimal totalPlatformCommission;

    private BigDecimal totalOwnerEarnings;

    private String currency;
}
