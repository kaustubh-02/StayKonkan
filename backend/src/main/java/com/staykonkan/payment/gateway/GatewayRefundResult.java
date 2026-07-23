package com.staykonkan.payment.gateway;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * Gateway-agnostic result of initiating a refund — mirrors
 * {@link GatewayOrderResult}'s reasoning: RefundServiceImpl never
 * touches Razorpay's Refund/JSONObject SDK types directly.
 */
@Getter
@Builder
public class GatewayRefundResult {

    private final String gatewayRefundId;

    private final BigDecimal amount;

    private final String gatewayStatus;
}
