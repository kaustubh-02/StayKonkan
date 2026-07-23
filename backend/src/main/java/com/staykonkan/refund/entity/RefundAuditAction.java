package com.staykonkan.refund.entity;

/** The four audit events explicitly required for Module 10C. */
public enum RefundAuditAction {
    REFUND_REQUESTED,
    REFUND_APPROVED,
    REFUND_FAILED,
    REFUND_COMPLETED
}
