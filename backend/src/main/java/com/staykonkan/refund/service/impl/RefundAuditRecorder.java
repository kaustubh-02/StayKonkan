package com.staykonkan.refund.service.impl;

import com.staykonkan.exception.ResourceNotFoundException;
import com.staykonkan.refund.entity.Refund;
import com.staykonkan.refund.entity.RefundAuditAction;
import com.staykonkan.refund.entity.RefundAuditLog;
import com.staykonkan.refund.entity.RefundStatus;
import com.staykonkan.refund.repository.RefundAuditLogRepository;
import com.staykonkan.refund.repository.RefundRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Every method here commits in its own transaction (REQUIRES_NEW), same
 * reasoning and same self-invocation-avoidance-via-separate-bean pattern
 * as WebhookAuditRecorder (Module 10B) and PaymentStatusUpdater (Module
 * 10A): the fact that a refund was requested — and the audit trail of
 * what happened to it — must survive even if the subsequent gateway
 * call fails. Without this, a failed gateway call would roll back the
 * REQUESTED row along with it, leaving no record the attempt ever
 * happened.
 */
@Service
@RequiredArgsConstructor
public class RefundAuditRecorder {

    private final RefundRepository refundRepository;
    private final RefundAuditLogRepository refundAuditLogRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Refund persistNewRefund(Refund refund) {
        return refundRepository.saveAndFlush(refund);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateOutcome(Long refundId, String razorpayRefundId, RefundStatus status, Instant processedAt) {
        Refund refund = refundRepository.findById(refundId)
                .orElseThrow(() -> ResourceNotFoundException.of("Refund", refundId));
        if (razorpayRefundId != null) {
            refund.setRazorpayRefundId(razorpayRefundId);
        }
        refund.setStatus(status);
        if (processedAt != null) {
            refund.setProcessedAt(processedAt);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void audit(Long refundId, RefundAuditAction action, String details) {
        Refund refund = refundRepository.getReferenceById(refundId);
        RefundAuditLog log = RefundAuditLog.builder()
                .refund(refund)
                .action(action)
                .details(details)
                .build();
        refundAuditLogRepository.save(log);
    }
}
