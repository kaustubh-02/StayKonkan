package com.staykonkan.webhook.service;

public interface WebhookService {

    /**
     * @param rawPayload      the exact, unmodified request body
     * @param signatureHeader the X-Razorpay-Signature header value (may be null/absent)
     */
    void processWebhook(String rawPayload, String signatureHeader);
}
