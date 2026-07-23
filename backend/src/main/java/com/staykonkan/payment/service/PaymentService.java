package com.staykonkan.payment.service;

import com.staykonkan.dto.PageResponseDTO;
import com.staykonkan.payment.dto.CreatePaymentRequest;
import com.staykonkan.payment.dto.PaymentResponse;
import com.staykonkan.payment.dto.PaymentSummaryResponse;
import com.staykonkan.payment.dto.PaymentVerificationRequest;

public interface PaymentService {

    PaymentResponse createOrder(CreatePaymentRequest request);

    PaymentResponse verifyPayment(PaymentVerificationRequest request);

    PageResponseDTO<PaymentResponse> getPaymentHistory(int page, int size);

    PaymentSummaryResponse getPaymentSummary();

    PaymentResponse getPaymentByBooking(Long bookingId);
}
