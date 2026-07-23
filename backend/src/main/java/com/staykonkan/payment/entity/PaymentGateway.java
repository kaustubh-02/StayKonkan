package com.staykonkan.payment.entity;

/**
 * Which gateway processed a payment. RAZORPAY is the only implementation
 * in Module 10A; STRIPE/PAYPAL etc. can be added here later without a
 * schema change — see com.staykonkan.payment.gateway.PaymentGatewayService
 * for the abstraction business logic actually depends on.
 */
public enum PaymentGateway {
    RAZORPAY
}
