package com.staykonkan.webhook.entity;

/** The five audit events explicitly required for Module 10B. */
public enum WebhookAuditAction {
    RECEIVED,
    SIGNATURE_VERIFIED,
    PAYMENT_UPDATED,
    BOOKING_UPDATED,
    WEBHOOK_FAILED
}
