package com.staykonkan.webhook.service.impl;

import com.staykonkan.exception.ResourceNotFoundException;
import com.staykonkan.webhook.entity.WebhookAuditAction;
import com.staykonkan.webhook.entity.WebhookAuditLog;
import com.staykonkan.webhook.entity.WebhookEvent;
import com.staykonkan.webhook.entity.WebhookProcessingStatus;
import com.staykonkan.webhook.repository.WebhookAuditLogRepository;
import com.staykonkan.webhook.repository.WebhookEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Every method here commits in its own transaction (REQUIRES_NEW), same
 * reasoning and same self-invocation-avoidance pattern as
 * PaymentStatusUpdater (Module 10A): an audit trail — and the
 * WebhookEvent idempotency row itself — must survive even when a later
 * step in the same webhook delivery throws. Called from a separate bean
 * (WebhookServiceImpl), so the REQUIRES_NEW actually takes effect
 * (calling a @Transactional method on `this` would silently run in the
 * caller's existing transaction instead).
 */
@Service
@RequiredArgsConstructor
public class WebhookAuditRecorder {

    private final WebhookEventRepository webhookEventRepository;
    private final WebhookAuditLogRepository webhookAuditLogRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public WebhookEvent createEvent(String idempotencyKey, String rawPayload) {
        WebhookEvent event = WebhookEvent.builder()
                .idempotencyKey(idempotencyKey)
                .payload(rawPayload)
                .processingStatus(WebhookProcessingStatus.RECEIVED)
                .build();
        return webhookEventRepository.saveAndFlush(event);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateStatus(Long eventId, WebhookProcessingStatus status) {
        WebhookEvent event = webhookEventRepository.findById(eventId)
                .orElseThrow(() -> ResourceNotFoundException.of("WebhookEvent", eventId));
        event.setProcessingStatus(status);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordParsedEvent(Long eventId, String rawEventType, String gatewayOrderId, String gatewayPaymentId) {
        WebhookEvent event = webhookEventRepository.findById(eventId)
                .orElseThrow(() -> ResourceNotFoundException.of("WebhookEvent", eventId));
        event.setRawEventType(rawEventType);
        event.setGatewayOrderId(gatewayOrderId);
        event.setGatewayPaymentId(gatewayPaymentId);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void audit(Long eventId, WebhookAuditAction action, String details) {
        WebhookEvent event = webhookEventRepository.getReferenceById(eventId);
        WebhookAuditLog log = WebhookAuditLog.builder()
                .webhookEvent(event)
                .action(action)
                .details(details)
                .build();
        webhookAuditLogRepository.save(log);
    }
}
