package com.staykonkan.payment.service.impl;

import com.staykonkan.exception.ResourceNotFoundException;
import com.staykonkan.payment.entity.Payment;
import com.staykonkan.payment.entity.PaymentStatus;
import com.staykonkan.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * A payment succeeding at the gateway is an external fact — real money
 * moved. That fact must be durably recorded even if a later step in the
 * same request (confirming the booking / updating the availability
 * calendar) fails. This is a separate Spring bean specifically so
 * {@code REQUIRES_NEW} actually takes effect: calling a
 * {@code @Transactional} method on `this` from within the same class
 * bypasses the Spring proxy and silently runs in the caller's existing
 * transaction (the classic self-invocation pitfall) — going through a
 * different bean avoids that.
 * <p>
 * If booking confirmation subsequently fails, the payment correctly
 * remains recorded as SUCCESS while the booking stays unconfirmed — a
 * real-money/no-booking mismatch that the (future, not-yet-implemented)
 * Refunds module is the correct place to reconcile. This is a known,
 * deliberately scoped limitation of Module 10A, not an oversight.
 */
@Service
@RequiredArgsConstructor
public class PaymentStatusUpdater {

    private final PaymentRepository paymentRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Payment markSuccess(Long paymentId, String gatewayPaymentId, String gatewaySignature, String paymentMethod) {

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> ResourceNotFoundException.of("Payment", paymentId));

        payment.setGatewayPaymentId(gatewayPaymentId);
        payment.setGatewaySignature(gatewaySignature);
        if (paymentMethod != null) {
            payment.setPaymentMethod(paymentMethod);
        }
        payment.setPaymentStatus(PaymentStatus.SUCCESS);

        return paymentRepository.saveAndFlush(payment);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(Long paymentId) {
        paymentRepository.findById(paymentId)
                .ifPresent(payment -> payment.setPaymentStatus(PaymentStatus.FAILED));
    }
}
