package com.staykonkan.booking.mapper;

import com.staykonkan.booking.dto.BookingResponse;
import com.staykonkan.booking.entity.Booking;
import org.springframework.stereotype.Component;

@Component
public class BookingMapper {

    public BookingResponse toResponse(Booking booking) {

        return BookingResponse.builder()
                .id(booking.getId())
                .bookingCode(booking.getBookingCode())

                .propertyId(booking.getProperty().getId())
                .propertyTitle(booking.getProperty().getTitle())

                .guestId(booking.getGuest().getId())
                .guestName(booking.getGuest().getFullName())

                .checkInDate(booking.getCheckInDate())
                .checkOutDate(booking.getCheckOutDate())

                .guestCount(booking.getGuestCount())

                .pricePerNight(booking.getPricePerNight())
                .totalAmount(booking.getTotalAmount())

                .status(booking.getStatus())

                .specialRequests(booking.getSpecialRequests())

                .createdAt(booking.getCreatedAt())

                .build();
    }

}