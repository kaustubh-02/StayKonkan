package com.staykonkan.settlement.mapper;

import com.staykonkan.settlement.dto.SettlementResponse;
import com.staykonkan.settlement.entity.Settlement;
import org.springframework.stereotype.Component;

@Component
public class SettlementMapper {

    public SettlementResponse toResponse(Settlement settlement) {

        SettlementResponse.SettlementResponseBuilder builder = SettlementResponse.builder()
                .id(settlement.getId())
                .settlementReference(settlement.getSettlementReference())
                .bookingAmount(settlement.getBookingAmount())
                .platformCommissionPercentage(settlement.getPlatformCommissionPercentage())
                .platformCommissionAmount(settlement.getPlatformCommissionAmount())
                .settlementAmount(settlement.getSettlementAmount())
                .currency(settlement.getCurrency())
                .status(settlement.getStatus())
                .gatewayTransferId(settlement.getGatewayTransferId())
                .notes(settlement.getNotes())
                .initiatedAt(settlement.getInitiatedAt())
                .completedAt(settlement.getCompletedAt())
                .createdAt(settlement.getCreatedAt())
                .updatedAt(settlement.getUpdatedAt());

        if (settlement.getPayment() != null) {
            builder.paymentId(settlement.getPayment().getId());
        }
        if (settlement.getBooking() != null) {
            builder.bookingId(settlement.getBooking().getId());
            builder.bookingCode(settlement.getBooking().getBookingCode());
        }
        if (settlement.getProperty() != null) {
            builder.propertyId(settlement.getProperty().getId());
            builder.propertyTitle(settlement.getProperty().getTitle());
        }
        if (settlement.getOwner() != null) {
            builder.ownerId(settlement.getOwner().getId());
            builder.ownerName(settlement.getOwner().getFullName());
        }

        return builder.build();
    }
}
