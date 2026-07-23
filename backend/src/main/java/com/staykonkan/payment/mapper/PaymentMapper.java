package com.staykonkan.payment.mapper;

import com.staykonkan.payment.dto.PaymentResponse;
import com.staykonkan.payment.entity.Payment;
import org.springframework.stereotype.Component;

@Component
public class PaymentMapper {

    public PaymentResponse toResponse(Payment payment) {

        PaymentResponse.PaymentResponseBuilder builder = PaymentResponse.builder()
                .id(payment.getId())
                .gatewayOrderId(payment.getGatewayOrderId())
                .gatewayPaymentId(payment.getGatewayPaymentId())
                // gatewaySignature intentionally omitted — see PaymentResponse Javadoc
                .bookingAmount(payment.getBookingAmount())
                .platformCommissionPercentage(payment.getPlatformCommissionPercentage())
                .platformCommissionAmount(payment.getPlatformCommissionAmount())
                .ownerAmount(payment.getOwnerAmount())
                .currency(payment.getCurrency())
                .paymentGateway(payment.getPaymentGateway())
                .paymentMethod(payment.getPaymentMethod())
                .paymentStatus(payment.getPaymentStatus())
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt());

        if (payment.getBooking() != null) {
            builder.bookingId(payment.getBooking().getId());
            builder.bookingCode(payment.getBooking().getBookingCode());
        }
        if (payment.getUser() != null) {
            builder.userId(payment.getUser().getId());
        }
        if (payment.getProperty() != null) {
            builder.propertyId(payment.getProperty().getId());
            builder.propertyTitle(payment.getProperty().getTitle());
        }

        return builder.build();
    }
}
