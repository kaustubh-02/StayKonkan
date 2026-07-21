package com.staykonkan.booking.service;

import com.staykonkan.booking.dto.BookingResponse;
import com.staykonkan.booking.dto.CreateBookingRequest;
import com.staykonkan.booking.dto.UpdateBookingStatusRequest;
import com.staykonkan.dto.PageResponseDTO;

public interface BookingService {

    BookingResponse createBooking(CreateBookingRequest request);

    BookingResponse getBookingById(Long bookingId);

    PageResponseDTO<BookingResponse> getMyBookings(
            int page,
            int size
    );

    PageResponseDTO<BookingResponse> getOwnerBookings(
            int page,
            int size
    );

    void cancelBooking(Long bookingId);

    BookingResponse updateBookingStatus(
            Long bookingId,
            UpdateBookingStatusRequest request
    );
}