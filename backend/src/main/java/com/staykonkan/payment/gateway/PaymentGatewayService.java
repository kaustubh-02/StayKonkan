package com.staykonkan.payment.gateway;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Business logic (PaymentServiceImpl) depends only on this interface,
 * never on any gateway SDK type directly. Adding Stripe/PayPal later
 * means writing a new implementation of this interface and switching
 * which bean is wired in — PaymentServiceImpl does not change.
 */
public interface PaymentGatewayService {

    /**
     * Creates an order with the gateway for the given amount (in the
     * currency's major unit, e.g. rupees — implementations handle any
     * minor-unit conversion the gateway requires internally).
     */
    GatewayOrderResult createOrder(BigDecimal amount, String currency, String receipt);

    /**
     * Cryptographically verifies that a (orderId, paymentId, signature)
     * triple genuinely came from the gateway — this is the ONLY thing
     * that may be trusted to mean "the customer paid". Never infer
     * success from any client-supplied status field.
     */
    boolean verifySignature(String gatewayOrderId, String gatewayPaymentId, String gatewaySignature);

    /** Best-effort; empty if the gateway call fails or the method isn't available (non-fatal — see callers). */
    Optional<String> fetchPaymentMethod(String gatewayPaymentId);

    /**
     * Initiates a refund against an already-captured payment (Module
     * 10C). Amount is in the currency's major unit, same convention as
     * {@link #createOrder}.
     */
    GatewayRefundResult initiateRefund(String gatewayPaymentId, BigDecimal amount, String receipt);
}
