package com.staykonkan.booking.controller;

import com.staykonkan.booking.dto.BookingResponse;
import com.staykonkan.booking.dto.CreateBookingRequest;
import com.staykonkan.booking.dto.UpdateBookingStatusRequest;
import com.staykonkan.booking.service.BookingService;
import com.staykonkan.constant.AppConstants;
import com.staykonkan.dto.PageResponseDTO;
import com.staykonkan.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * Booking endpoints. Every action here is scoped down further inside
 * BookingServiceImpl (guest/property-owner/admin ownership checks) since
 * "who may act on booking X" depends on data (who the guest is, who owns
 * the property), not just the caller's role — so plain @PreAuthorize at
 * the controller level isn't expressive enough, matching how Property's
 * per-record checks are also pushed into the service layer.
 */
@RestController
@RequestMapping(AppConstants.API_V1 + "/bookings")
@Tag(name = "Bookings", description = "Create and manage property bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new booking for a property")
    public ApiResponse<BookingResponse> createBooking(@Valid @RequestBody CreateBookingRequest request) {
        return ApiResponse.ok(bookingService.createBooking(request), "Booking created successfully");
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a booking by id (guest, property owner, or admin only)")
    public ApiResponse<BookingResponse> getBooking(@PathVariable Long id) {
        return ApiResponse.ok(bookingService.getBookingById(id));
    }

    @GetMapping("/my")
    @Operation(summary = "List the current user's own bookings (as a guest)")
    public ApiResponse<PageResponseDTO<BookingResponse>> getMyBookings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(bookingService.getMyBookings(page, size));
    }

    @GetMapping("/owner")
    @Operation(summary = "List bookings made against the current user's properties")
    public ApiResponse<PageResponseDTO<BookingResponse>> getOwnerBookings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(bookingService.getOwnerBookings(page, size));
    }

    @PutMapping("/{id}/cancel")
    @Operation(summary = "Cancel a booking (guest, property owner, or admin only)")
    public ApiResponse<Void> cancelBooking(@PathVariable Long id) {
        bookingService.cancelBooking(id);
        return ApiResponse.message("Booking cancelled successfully");
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "Update a booking's status (property owner or admin only)")
    public ApiResponse<BookingResponse> updateBookingStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateBookingStatusRequest request) {
        return ApiResponse.ok(bookingService.updateBookingStatus(id, request), "Booking status updated successfully");
    }
}
