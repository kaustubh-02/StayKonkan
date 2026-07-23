package com.staykonkan.webhook.entity;

/** Lifecycle of a single inbound webhook delivery, tracked independently of PaymentStatus/BookingStatus. */
public enum WebhookProcessingStatus {
    RECEIVED,
    PROCESSED,
    IGNORED,
    FAILED
}
