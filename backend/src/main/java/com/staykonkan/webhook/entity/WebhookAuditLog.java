package com.staykonkan.webhook.entity;

import com.staykonkan.entity.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * An append-only trail of what happened while processing one
 * WebhookEvent. Written via WebhookAuditRecorder in its own committed
 * transaction per entry (REQUIRES_NEW), specifically so the trail
 * survives even when a later step in the same webhook delivery fails —
 * an audit log that disappears exactly when something goes wrong is not
 * useful.
 */
@Entity
@Table(name = "webhook_audit_logs")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebhookAuditLog extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "webhook_event_id", nullable = false)
    private WebhookEvent webhookEvent;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private WebhookAuditAction action;

    @Column(length = 1000)
    private String details;
}
