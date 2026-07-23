package com.staykonkan.payment.gateway;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * Gateway-agnostic result of creating an order — deliberately not the
 * raw Razorpay SDK response type, so PaymentServiceImpl never touches
 * Razorpay's JSONObject/Order classes directly.
 */
@Getter
@Builder
public class GatewayOrderResult {

    private final String gatewayOrderId;

    private final BigDecimal amount;

    private final String currency;

    private final String gatewayStatus;
}
