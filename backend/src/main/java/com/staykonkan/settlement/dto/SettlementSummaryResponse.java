package com.staykonkan.settlement.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Scope adapts to the caller's role, same pattern as
 * PaymentSummaryResponse (Module 10A): an OWNER sees their own
 * settlement totals, an ADMIN sees the platform-wide picture. Totals
 * cover COMPLETED settlements only.
 */
@Getter
@Setter
@Builder
public class SettlementSummaryResponse {

    private long completedSettlementCount;

    private BigDecimal totalSettledAmount;

    private BigDecimal totalPlatformCommission;

    private BigDecimal pendingSettlementAmount;

    private String currency;
}
