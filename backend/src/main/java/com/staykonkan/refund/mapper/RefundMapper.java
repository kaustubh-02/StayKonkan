package com.staykonkan.refund.mapper;

import com.staykonkan.refund.dto.RefundResponse;
import com.staykonkan.refund.entity.Refund;
import org.springframework.stereotype.Component;

@Component
public class RefundMapper {

    public RefundResponse toResponse(Refund refund) {

        RefundResponse.RefundResponseBuilder builder = RefundResponse.builder()
                .id(refund.getId())
                .refundReference(refund.getRefundReference())
                .razorpayRefundId(refund.getRazorpayRefundId())
                .refundAmount(refund.getRefundAmount())
                .refundReason(refund.getRefundReason())
                .status(refund.getStatus())
                .requestedAt(refund.getRequestedAt())
                .processedAt(refund.getProcessedAt())
                .createdAt(refund.getCreatedAt())
                .updatedAt(refund.getUpdatedAt());

        if (refund.getPayment() != null) {
            builder.paymentId(refund.getPayment().getId());
        }
        if (refund.getBooking() != null) {
            builder.bookingId(refund.getBooking().getId());
            builder.bookingCode(refund.getBooking().getBookingCode());
        }
        if (refund.getUser() != null) {
            builder.userId(refund.getUser().getId());
        }

        return builder.build();
    }
}
