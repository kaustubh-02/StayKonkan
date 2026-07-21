package com.staykonkan.booking.dto;

import com.staykonkan.booking.entity.BookingStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
@Builder
public class BookingResponse {

    private Long id;

    private String bookingCode;

    private Long propertyId;

    private String propertyTitle;

    private Long guestId;

    private String guestName;

    private LocalDate checkInDate;

    private LocalDate checkOutDate;

    private Integer guestCount;

    private BigDecimal pricePerNight;

    private BigDecimal totalAmount;

    private BookingStatus status;

    private String specialRequests;

    private Instant createdAt;
}