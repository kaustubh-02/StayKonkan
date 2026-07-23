package com.staykonkan.webhook.service.impl;

import com.staykonkan.booking.service.BookingService;
import com.staykonkan.exception.ResourceNotFoundException;
import com.staykonkan.exception.ValidationException;
import com.staykonkan.payment.entity.Payment;
import com.staykonkan.payment.entity.PaymentStatus;
import com.staykonkan.payment.repository.PaymentRepository;
import com.staykonkan.payment.service.impl.PaymentStatusUpdater;
import com.staykonkan.webhook.entity.WebhookAuditAction;
import com.staykonkan.webhook.entity.WebhookEvent;
import com.staykonkan.webhook.entity.WebhookProcessingStatus;
import com.staykonkan.webhook.repository.WebhookEventRepository;
import com.staykonkan.webhook.security.WebhookSignatureVerifier;
import com.staykonkan.webhook.service.WebhookService;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;

/**
 * Orchestrates inbound Razorpay webhook processing. Deliberately NOT
 * itself @Transactional: every meaningful write goes through a
 * collaborator that manages its own transaction —
 * WebhookAuditRecorder (REQUIRES_NEW, Module 10B) for event/audit rows,
 * PaymentStatusUpdater (REQUIRES_NEW, Module 10A, reused unmodified) for
 * the payment SUCCESS write, and BookingService.confirmAfterPayment
 * (Module 10A, reused unmodified) for booking + availability. This way
 * a failure at any step still leaves a truthful, durable trail of
 * exactly what happened, instead of one failure erasing everything that
 * came before it in the same request.
 */
@Service
@RequiredArgsConstructor
public class WebhookServiceImpl implements WebhookService {

    private static final Logger log = LoggerFactory.getLogger(WebhookServiceImpl.class);

    private static final String EVENT_PAYMENT_CAPTURED = "payment.captured";
    private static final String EVENT_PAYMENT_FAILED = "payment.failed";
    private static final String EVENT_ORDER_PAID = "order.paid";

    private final WebhookEventRepository webhookEventRepository;
    private final WebhookSignatureVerifier signatureVerifier;
    private final WebhookAuditRecorder auditRecorder;
    private final PaymentRepository paymentRepository;
    private final PaymentStatusUpdater paymentStatusUpdater;
    private final BookingService bookingService;

    @Override
    public void processWebhook(String rawPayload, String signatureHeader) {

        String idempotencyKey = computeIdempotencyKey(rawPayload);

        Optional<WebhookEvent> existing = webhookEventRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            // Byte-identical retry of a delivery we've already handled
            // (Razorpay retries on any non-2xx, and may also occasionally
            // redeliver even after a 2xx) — do not reprocess.
            auditRecorder.audit(existing.get().getId(), WebhookAuditAction.RECEIVED,
                    "Duplicate delivery (already " + existing.get().getProcessingStatus() + ") — skipped");
            log.info("Ignoring duplicate webhook delivery, idempotencyKey={}", idempotencyKey);
            return;
        }

        WebhookEvent event = auditRecorder.createEvent(idempotencyKey, rawPayload);
        auditRecorder.audit(event.getId(), WebhookAuditAction.RECEIVED,
                "Webhook received (" + rawPayload.length() + " bytes)");

        // Signature verified BEFORE a single field of the payload is
        // parsed/trusted — this is the crux of "never trust webhook
        // payload without verification".
        boolean signatureValid = signatureVerifier.verify(rawPayload, signatureHeader);

        if (!signatureValid) {
            auditRecorder.updateStatus(event.getId(), WebhookProcessingStatus.FAILED);
            auditRecorder.audit(event.getId(), WebhookAuditAction.WEBHOOK_FAILED, "Signature verification failed");
            throw new ValidationException("Invalid webhook signature");
        }

        auditRecorder.audit(event.getId(), WebhookAuditAction.SIGNATURE_VERIFIED, "Signature verified successfully");

        try {
            dispatch(event, rawPayload);
        } catch (RuntimeException e) {
            auditRecorder.updateStatus(event.getId(), WebhookProcessingStatus.FAILED);
            auditRecorder.audit(event.getId(), WebhookAuditAction.WEBHOOK_FAILED,
                    "Processing failed: " + e.getMessage());
            throw e;
        }
    }

    private void dispatch(WebhookEvent event, String rawPayload) {

        JSONObject root;
        try {
            root = new JSONObject(rawPayload);
        } catch (org.json.JSONException e) {
            throw new ValidationException("Webhook body is not valid JSON");
        }

        String eventType = root.optString("event", null);

        JSONObject payloadWrapper = root.optJSONObject("payload");
        JSONObject paymentEntity = payloadWrapper != null ? payloadWrapper.optJSONObject("payment") : null;
        JSONObject paymentDetails = paymentEntity != null ? paymentEntity.optJSONObject("entity") : null;

        String gatewayPaymentId = paymentDetails != null ? paymentDetails.optString("id", null) : null;
        String gatewayOrderId = paymentDetails != null ? paymentDetails.optString("order_id", null) : null;
        String paymentMethod = paymentDetails != null ? paymentDetails.optString("method", null) : null;

        auditRecorder.recordParsedEvent(event.getId(), eventType, gatewayOrderId, gatewayPaymentId);

        if (eventType == null) {
            auditRecorder.updateStatus(event.getId(), WebhookProcessingStatus.FAILED);
            auditRecorder.audit(event.getId(), WebhookAuditAction.WEBHOOK_FAILED, "Payload had no 'event' field");
            return;
        }

        switch (eventType) {
            case EVENT_PAYMENT_CAPTURED -> handlePaymentCaptured(event, gatewayOrderId, gatewayPaymentId, paymentMethod);
            case EVENT_PAYMENT_FAILED -> handlePaymentFailed(event, gatewayOrderId);
            case EVENT_ORDER_PAID -> handleOrderPaid(event, gatewayOrderId);
            default -> {
                auditRecorder.updateStatus(event.getId(), WebhookProcessingStatus.IGNORED);
                auditRecorder.audit(event.getId(), WebhookAuditAction.RECEIVED,
                        "Unsupported event type '" + eventType + "' — ignored safely");
                log.info("Ignoring unsupported webhook event type '{}'", eventType);
            }
        }
    }

    private void handlePaymentCaptured(WebhookEvent event, String gatewayOrderId, String gatewayPaymentId, String paymentMethod) {

        if (gatewayOrderId == null) {
            auditRecorder.updateStatus(event.getId(), WebhookProcessingStatus.FAILED);
            auditRecorder.audit(event.getId(), WebhookAuditAction.WEBHOOK_FAILED,
                    "payment.captured payload had no order_id");
            return;
        }

        Payment payment = paymentRepository.findByGatewayOrderId(gatewayOrderId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No payment found for gateway order id " + gatewayOrderId));

        if (payment.getPaymentStatus() == PaymentStatus.SUCCESS) {
            // Already confirmed — most likely via the synchronous
            // POST /verify flow (Module 10A) beating this webhook to it,
            // which is the expected common case, not an error.
            auditRecorder.audit(event.getId(), WebhookAuditAction.PAYMENT_UPDATED,
                    "Payment already SUCCESS — no change (likely already confirmed via /verify)");
            auditRecorder.updateStatus(event.getId(), WebhookProcessingStatus.PROCESSED);
            return;
        }

        if (payment.getPaymentStatus() != PaymentStatus.ORDER_CREATED) {
            auditRecorder.audit(event.getId(), WebhookAuditAction.WEBHOOK_FAILED,
                    "payment.captured received but payment status is " + payment.getPaymentStatus()
                            + " (expected ORDER_CREATED) — not applying");
            auditRecorder.updateStatus(event.getId(), WebhookProcessingStatus.FAILED);
            return;
        }

        // Reuses Module 10A's PaymentStatusUpdater unmodified — same
        // durable-commit-then-confirm pattern as the synchronous /verify
        // flow. No razorpay_signature is available on this path (that's
        // a checkout-only artifact); passing null is the intended,
        // schema-safe use of that nullable column here.
        Payment updated = paymentStatusUpdater.markSuccess(payment.getId(), gatewayPaymentId, null, paymentMethod);
        auditRecorder.audit(event.getId(), WebhookAuditAction.PAYMENT_UPDATED, "Payment marked SUCCESS via webhook");

        try {
            bookingService.confirmAfterPayment(updated.getBooking().getId());
            auditRecorder.audit(event.getId(), WebhookAuditAction.BOOKING_UPDATED,
                    "Booking confirmed and availability updated via webhook");
            auditRecorder.updateStatus(event.getId(), WebhookProcessingStatus.PROCESSED);
        } catch (RuntimeException e) {
            // Payment SUCCESS is already durably committed above and
            // stays that way — see WebhookServiceImpl class Javadoc and
            // PaymentServiceImpl.verifyPayment for the same reasoning.
            // This is a real-money/no-booking mismatch for the future
            // Refunds module to reconcile, not something to paper over.
            auditRecorder.audit(event.getId(), WebhookAuditAction.WEBHOOK_FAILED,
                    "Payment succeeded but booking confirmation failed: " + e.getMessage());
            throw e;
        }
    }

    private void handlePaymentFailed(WebhookEvent event, String gatewayOrderId) {

        if (gatewayOrderId == null) {
            auditRecorder.updateStatus(event.getId(), WebhookProcessingStatus.FAILED);
            auditRecorder.audit(event.getId(), WebhookAuditAction.WEBHOOK_FAILED,
                    "payment.failed payload had no order_id");
            return;
        }

        Payment payment = paymentRepository.findByGatewayOrderId(gatewayOrderId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No payment found for gateway order id " + gatewayOrderId));

        if (payment.getPaymentStatus() == PaymentStatus.SUCCESS) {
            // Never downgrade an already-captured payment because of a
            // later failed retry notification for the same order.
            auditRecorder.audit(event.getId(), WebhookAuditAction.PAYMENT_UPDATED,
                    "payment.failed received but payment is already SUCCESS — ignored");
            auditRecorder.updateStatus(event.getId(), WebhookProcessingStatus.PROCESSED);
            return;
        }

        if (payment.getPaymentStatus() == PaymentStatus.FAILED || payment.getPaymentStatus() == PaymentStatus.CANCELLED) {
            auditRecorder.audit(event.getId(), WebhookAuditAction.PAYMENT_UPDATED,
                    "Payment already " + payment.getPaymentStatus() + " — no change");
            auditRecorder.updateStatus(event.getId(), WebhookProcessingStatus.PROCESSED);
            return;
        }

        paymentStatusUpdater.markFailed(payment.getId());
        auditRecorder.audit(event.getId(), WebhookAuditAction.PAYMENT_UPDATED, "Payment marked FAILED via webhook");

        // "Release blocked availability if required" — nothing to
        // release here: per Module 9's design, availability rows are
        // only ever created at CONFIRMED time (PropertyAvailabilityService
        // .bookDates, called from BookingService.confirmAfterPayment), so
        // a payment that never reached SUCCESS never reserved any
        // calendar dates in the first place. The booking itself simply
        // remains PENDING, exactly as Modules 4/10A already designed it.
        auditRecorder.audit(event.getId(), WebhookAuditAction.BOOKING_UPDATED,
                "No booking/availability change needed — booking remains PENDING (payment never reserved dates)");
        auditRecorder.updateStatus(event.getId(), WebhookProcessingStatus.PROCESSED);
    }

    private void handleOrderPaid(WebhookEvent event, String gatewayOrderId) {
        // order.paid is Razorpay's order-level confirmation and normally
        // accompanies a payment.captured event for the same transaction.
        // payment.captured is treated as the single authoritative trigger
        // for booking confirmation (above) — driving the same side effect
        // from two separate events would risk racing itself for no
        // benefit, so this event is acknowledged and audited only.
        auditRecorder.audit(event.getId(), WebhookAuditAction.PAYMENT_UPDATED,
                "order.paid acknowledged for order " + gatewayOrderId
                        + " — no independent action taken (payment.captured is authoritative)");
        auditRecorder.updateStatus(event.getId(), WebhookProcessingStatus.PROCESSED);
    }

    private String computeIdempotencyKey(String rawPayload) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawPayload.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is a JDK-guaranteed algorithm (every conformant JVM
            // ships it) — this branch is unreachable in practice.
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
