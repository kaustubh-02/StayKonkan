package com.staykonkan.refund.entity;

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
 * Append-only trail of what happened to one Refund. Written via
 * RefundAuditRecorder in its own committed transaction per entry
 * (REQUIRES_NEW) — same durability reasoning as Module 10B's
 * WebhookAuditLog/WebhookAuditRecorder: an audit log must survive the
 * failure it's recording.
 */
@Entity
@Table(name = "refund_audit_logs")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundAuditLog extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "refund_id", nullable = false)
    private Refund refund;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RefundAuditAction action;

    @Column(length = 1000)
    private String details;
}
