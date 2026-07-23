package com.staykonkan.webhook.security;

import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import com.staykonkan.config.RazorpayWebhookConfig;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Verifies that a webhook delivery genuinely came from Razorpay.
 * <p>
 * This is a DIFFERENT algorithm from checkout payment verification
 * (RazorpayPaymentService.verifySignature, Module 10A): webhooks sign
 * the raw request body with the separate webhook secret, not
 * "order_id|payment_id" with the API key secret. Deliberately not added
 * to PaymentGatewayService (Module 10A) — that abstraction models the
 * checkout flow, and conflating the two signing schemes into one
 * interface method would be a correctness risk, not a simplification.
 */
@Component
@RequiredArgsConstructor
public class WebhookSignatureVerifier {

    private static final Logger log = LoggerFactory.getLogger(WebhookSignatureVerifier.class);

    private final RazorpayWebhookConfig.RazorpayWebhookProperties webhookProperties;

    /**
     * @param rawPayload the exact, unmodified request body bytes (as a
     *                    string) — re-serializing a parsed JSON object
     *                    before verifying would very likely change byte
     *                    ordering/whitespace and break the signature.
     */
    public boolean verify(String rawPayload, String signatureHeader) {
        if (signatureHeader == null || signatureHeader.isBlank()) {
            log.warn("Webhook request missing X-Razorpay-Signature header");
            return false;
        }
        if (rawPayload == null || rawPayload.isBlank()) {
            log.warn("Webhook request had an empty body");
            return false;
        }
        try {
            return Utils.verifyWebhookSignature(rawPayload, signatureHeader, webhookProperties.getSecret());
        } catch (RazorpayException e) {
            log.warn("Webhook signature verification threw — treating as verification failure", e);
            return false;
        }
    }
}
