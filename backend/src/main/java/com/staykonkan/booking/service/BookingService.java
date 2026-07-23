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

    /**
     * Transitions a PENDING booking straight to CONFIRMED as a result of
     * successful payment verification (Module 10A) — distinct from
     * updateBookingStatus, which is the owner/admin-driven manual path
     * from Module 4 and is left untouched. Not exposed via any
     * controller; called only from PaymentServiceImpl after a payment's
     * gateway signature has been verified.
     */
    BookingResponse confirmAfterPayment(Long bookingId);
}