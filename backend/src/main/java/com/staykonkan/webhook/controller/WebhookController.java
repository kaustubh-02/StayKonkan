package com.staykonkan.webhook.controller;

import com.staykonkan.constant.AppConstants;
import com.staykonkan.response.ApiResponse;
import com.staykonkan.webhook.service.WebhookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Public endpoint (see SecurityConstants.PUBLIC_ENDPOINTS) — called
 * directly by Razorpay's servers, never by a logged-in StayKonkan user.
 * Trust comes entirely from WebhookSignatureVerifier, not from JWT.
 * <p>
 * The body is read manually from the raw request stream rather than via
 * a typed @RequestBody DTO: signature verification needs the exact,
 * byte-for-byte original payload, and routing it through Spring's
 * Jackson message converter risks re-serialization changing whitespace/
 * key order, which would silently break every signature check.
 */
@RestController
@RequestMapping(AppConstants.API_V1 + "/payments/webhook")
@Tag(name = "Payment Webhooks", description = "Razorpay server-to-server webhook receiver (Module 10B)")
public class WebhookController {

    private static final String SIGNATURE_HEADER = "X-Razorpay-Signature";

    private final WebhookService webhookService;

    public WebhookController(WebhookService webhookService) {
        this.webhookService = webhookService;
    }

    @PostMapping
    @Operation(summary = "Receive a Razorpay webhook event",
            description = "Handles payment.captured, payment.failed, and order.paid; other event types are " +
                    "acknowledged and ignored safely. The signature is verified against the raw body before " +
                    "any payload content is trusted. Duplicate deliveries (retries) are detected and skipped.")
    public ApiResponse<Void> receiveWebhook(
            HttpServletRequest request,
            @Parameter(description = "Razorpay's HMAC signature of the raw request body")
            @RequestHeader(value = SIGNATURE_HEADER, required = false) String signature) throws IOException {

        String rawPayload = new String(request.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

        webhookService.processWebhook(rawPayload, signature);

        return ApiResponse.message("Webhook processed");
    }
}
