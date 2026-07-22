package com.staykonkan.availability.controller;

import com.staykonkan.availability.dto.AvailabilityCalendarResponse;
import com.staykonkan.availability.dto.AvailabilityResponse;
import com.staykonkan.availability.dto.UpdateAvailabilityRequest;
import com.staykonkan.availability.service.PropertyAvailabilityService;
import com.staykonkan.constant.AppConstants;
import com.staykonkan.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Read endpoints (availability list, calendar, check) are open to any
 * authenticated user, including guests — they only browse. Block/release
 * are restricted to the property owner or an admin, enforced inside
 * PropertyAvailabilityServiceImpl since it depends on data (who owns the
 * property), matching the pattern used by every other module's
 * per-record checks (Booking, Review, Wishlist, PropertyImage, Amenity).
 */
@RestController
@RequestMapping(AppConstants.API_V1 + "/availability")
@Tag(name = "Availability", description = "Property availability calendar and double-booking prevention")
public class PropertyAvailabilityController {

    private final PropertyAvailabilityService availabilityService;

    public PropertyAvailabilityController(PropertyAvailabilityService availabilityService) {
        this.availabilityService = availabilityService;
    }

    @GetMapping("/property/{propertyId}")
    @Operation(summary = "Get persisted availability exceptions for a property",
            description = "Returns only dates with a recorded status (booked/blocked/maintenance); a date not " +
                    "present in the response is implicitly available")
    public ApiResponse<List<AvailabilityResponse>> getAvailability(
            @PathVariable Long propertyId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ApiResponse.ok(availabilityService.getAvailability(propertyId, startDate, endDate));
    }

    @GetMapping("/property/{propertyId}/calendar")
    @Operation(summary = "Get a full day-by-day availability calendar for a property")
    public ApiResponse<AvailabilityCalendarResponse> getCalendar(
            @PathVariable Long propertyId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ApiResponse.ok(availabilityService.getCalendar(propertyId, startDate, endDate));
    }

    @GetMapping("/property/{propertyId}/check")
    @Operation(summary = "Check whether a property is available for a stay",
            description = "checkOutDate is exclusive, matching booking semantics (a 2-night stay is checkIn=D1, checkOut=D3)")
    public ApiResponse<Boolean> checkAvailability(
            @PathVariable Long propertyId,
            @Parameter(description = "Inclusive")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkInDate,
            @Parameter(description = "Exclusive")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOutDate) {
        return ApiResponse.ok(availabilityService.checkAvailability(propertyId, checkInDate, checkOutDate));
    }

    @PutMapping("/property/{propertyId}/block")
    @Operation(summary = "Block dates on a property (owner or admin only)",
            description = "status must be BLOCKED or MAINTENANCE. startDate/endDate are both inclusive")
    public ApiResponse<List<AvailabilityResponse>> blockDates(
            @PathVariable Long propertyId,
            @Valid @RequestBody UpdateAvailabilityRequest request) {
        return ApiResponse.ok(availabilityService.blockDates(propertyId, request), "Dates blocked successfully");
    }

    @PutMapping("/property/{propertyId}/release")
    @Operation(summary = "Release previously blocked dates back to available (owner or admin only)",
            description = "Fails if any date in range has a confirmed booking — cancel the booking instead")
    public ApiResponse<List<AvailabilityResponse>> releaseDates(
            @PathVariable Long propertyId,
            @Valid @RequestBody UpdateAvailabilityRequest request) {
        return ApiResponse.ok(availabilityService.releaseDates(propertyId, request), "Dates released successfully");
    }
}
