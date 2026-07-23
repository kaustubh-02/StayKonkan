package com.staykonkan.payment.entity;

/**
 * PENDING is reserved for gateway flows where an order can exist in a
 * not-yet-confirmed state before the gateway acknowledges it. The
 * synchronous Razorpay order-create flow implemented in Module 10A does
 * not emit it — a Payment row is only persisted once Razorpay has
 * actually returned an order id, straight into ORDER_CREATED — but the
 * status remains available for future/alternate gateway flows.
 */
public enum PaymentStatus {
    PENDING,
    ORDER_CREATED,
    SUCCESS,
    FAILED,
    CANCELLED
}
