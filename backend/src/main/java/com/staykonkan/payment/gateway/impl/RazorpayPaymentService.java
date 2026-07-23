package com.staykonkan.payment.gateway.impl;

import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import com.staykonkan.config.RazorpayConfig;
import com.staykonkan.exception.ExternalServiceException;
import com.staykonkan.payment.gateway.GatewayOrderResult;
import com.staykonkan.payment.gateway.GatewayRefundResult;
import com.staykonkan.payment.gateway.PaymentGatewayService;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * The only class in the codebase that touches the Razorpay SDK directly.
 * Everything else talks to {@link PaymentGatewayService}.
 */
@Service
@RequiredArgsConstructor
public class RazorpayPaymentService implements PaymentGatewayService {

    private static final Logger log = LoggerFactory.getLogger(RazorpayPaymentService.class);

    // Razorpay amounts are in the currency's smallest unit (e.g. paise for INR).
    private static final int MINOR_UNIT_MULTIPLIER = 100;

    private final RazorpayClient razorpayClient;
    private final RazorpayConfig.RazorpayProperties razorpayProperties;

    @Override
    public GatewayOrderResult createOrder(BigDecimal amount, String currency, String receipt) {
        try {
            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", amount.multiply(BigDecimal.valueOf(MINOR_UNIT_MULTIPLIER)).longValueExact());
            orderRequest.put("currency", currency);
            orderRequest.put("receipt", receipt);
            orderRequest.put("payment_capture", 1);

            com.razorpay.Order order = razorpayClient.orders.create(orderRequest);

            return GatewayOrderResult.builder()
                    .gatewayOrderId(order.get("id"))
                    .amount(amount)
                    .currency(currency)
                    .gatewayStatus(order.get("status"))
                    .build();

        } catch (RazorpayException e) {
            // Never log the raw exception message unfiltered in case it
            // ever echoes request payload details; log a fixed message
            // plus the type, keep the stack trace server-side only.
            log.error("Razorpay order creation failed", e);
            throw new ExternalServiceException("Failed to create payment order with the gateway", e);
        }
    }

    @Override
    public boolean verifySignature(String gatewayOrderId, String gatewayPaymentId, String gatewaySignature) {
        try {
            JSONObject options = new JSONObject();
            options.put("razorpay_order_id", gatewayOrderId);
            options.put("razorpay_payment_id", gatewayPaymentId);
            options.put("razorpay_signature", gatewaySignature);

            // Local HMAC-SHA256 recomputation against the key secret — no
            // network call, and the only trustworthy source of truth for
            // "did this payment actually happen".
            return Utils.verifyPaymentSignature(options, razorpayProperties.getKeySecret());

        } catch (RazorpayException e) {
            log.warn("Razorpay signature verification threw — treating as verification failure", e);
            return false;
        }
    }

    @Override
    public Optional<String> fetchPaymentMethod(String gatewayPaymentId) {
        try {
            com.razorpay.Payment payment = razorpayClient.payments.fetch(gatewayPaymentId);
            return Optional.ofNullable(payment.get("method"));
        } catch (Exception e) {
            // Best-effort metadata only — never let this fail the
            // verification flow that already succeeded.
            log.warn("Could not fetch payment method from Razorpay for {}", gatewayPaymentId, e);
            return Optional.empty();
        }
    }

    @Override
    public GatewayRefundResult initiateRefund(String gatewayPaymentId, BigDecimal amount, String receipt) {
        try {
            JSONObject refundRequest = new JSONObject();
            refundRequest.put("amount", amount.multiply(BigDecimal.valueOf(MINOR_UNIT_MULTIPLIER)).longValueExact());
            refundRequest.put("speed", "normal");
            if (receipt != null) {
                refundRequest.put("receipt", receipt);
            }

            com.razorpay.Refund refund = razorpayClient.payments.refund(gatewayPaymentId, refundRequest);

            return GatewayRefundResult.builder()
                    .gatewayRefundId(refund.get("id"))
                    .amount(amount)
                    .gatewayStatus(refund.get("status"))
                    .build();

        } catch (RazorpayException e) {
            log.error("Razorpay refund initiation failed", e);
            throw new ExternalServiceException("Failed to initiate refund with the gateway", e);
        }
    }
}
