package com.staykonkan.webhook.entity;

import com.staykonkan.entity.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * One row per inbound Razorpay webhook delivery. idempotencyKey is a
 * SHA-256 hash of the raw request body (see WebhookServiceImpl) —
 * deliberately not derived from a specific Razorpay header, since a
 * byte-identical retry (which is how Razorpay retries failed
 * deliveries) always hashes to the same key regardless of header
 * conventions, and this table's unique constraint on it is what
 * actually enforces "process at most once" at the DB level.
 */
@Entity
@Table(
        name = "webhook_events",
        uniqueConstraints = @UniqueConstraint(name = "uk_webhook_event_idempotency_key", columnNames = "idempotency_key")
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebhookEvent extends AuditableEntity {

    @Column(name = "idempotency_key", nullable = false, length = 64)
    private String idempotencyKey;

    // Null until the signature is verified — see WebhookServiceImpl:
    // event type is only trusted/parsed out of the payload after
    // verification succeeds.
    @Column(name = "raw_event_type", length = 50)
    private String rawEventType;

    @Column(name = "gateway_order_id", length = 100)
    private String gatewayOrderId;

    @Column(name = "gateway_payment_id", length = 100)
    private String gatewayPaymentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "processing_status", nullable = false, length = 20)
    @Builder.Default
    private WebhookProcessingStatus processingStatus = WebhookProcessingStatus.RECEIVED;

    // Raw body, stored verbatim for audit/reconciliation/replay-debugging
    // against the Razorpay dashboard. Only ever written once (on
    // receipt) and never displayed back through any API response.
    @Column(columnDefinition = "TEXT")
    private String payload;
}
