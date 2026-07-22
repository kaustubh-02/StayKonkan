package com.staykonkan.amenity.controller;

import com.staykonkan.amenity.dto.AmenityResponse;
import com.staykonkan.amenity.dto.AssignAmenitiesRequest;
import com.staykonkan.amenity.dto.CreateAmenityRequest;
import com.staykonkan.amenity.dto.UpdateAmenityRequest;
import com.staykonkan.amenity.entity.AmenityCategory;
import com.staykonkan.amenity.service.AmenityService;
import com.staykonkan.constant.AppConstants;
import com.staykonkan.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Amenity catalog management (ADMIN only) plus per-property
 * assignment (property owner or ADMIN, enforced in AmenityServiceImpl
 * since it depends on data — same pattern as Booking/Review/Wishlist/
 * PropertyImage controllers).
 */
@RestController
@RequestMapping(AppConstants.API_V1 + "/amenities")
@Tag(name = "Amenities", description = "Reusable amenity catalog and property amenity assignment")
public class AmenityController {

    private final AmenityService amenityService;

    public AmenityController(AmenityService amenityService) {
        this.amenityService = amenityService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new amenity (Admin only)")
    public ApiResponse<AmenityResponse> createAmenity(@Valid @RequestBody CreateAmenityRequest request) {
        return ApiResponse.ok(amenityService.createAmenity(request), "Amenity created successfully");
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update an amenity (Admin only)")
    public ApiResponse<AmenityResponse> updateAmenity(
            @PathVariable Long id,
            @Valid @RequestBody UpdateAmenityRequest request) {
        return ApiResponse.ok(amenityService.updateAmenity(id, request), "Amenity updated successfully");
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Deactivate an amenity (Admin only)", description = "Soft delete — the amenity is hidden from future selection but preserved for properties that already have it assigned")
    public ApiResponse<Void> deleteAmenity(@PathVariable Long id) {
        amenityService.deleteAmenity(id);
        return ApiResponse.message("Amenity deactivated successfully");
    }

    @GetMapping
    @Operation(summary = "List all amenities in the catalog")
    public ApiResponse<List<AmenityResponse>> getAllAmenities() {
        return ApiResponse.ok(amenityService.getAllAmenities());
    }

    @GetMapping("/category/{category}")
    @Operation(summary = "List amenities filtered by category")
    public ApiResponse<List<AmenityResponse>> getAmenitiesByCategory(@PathVariable AmenityCategory category) {
        return ApiResponse.ok(amenityService.getAmenitiesByCategory(category));
    }

    @PostMapping("/property/{propertyId}")
    @Operation(summary = "Assign amenities to a property", description = "Only the property owner or an admin may assign amenities")
    public ApiResponse<List<AmenityResponse>> assignAmenitiesToProperty(
            @PathVariable Long propertyId,
            @Valid @RequestBody AssignAmenitiesRequest request) {
        return ApiResponse.ok(amenityService.assignAmenitiesToProperty(propertyId, request), "Amenities assigned successfully");
    }

    @DeleteMapping("/property/{propertyId}/{amenityId}")
    @Operation(summary = "Remove an amenity from a property", description = "Only the property owner or an admin may remove amenities")
    public ApiResponse<Void> removeAmenityFromProperty(
            @PathVariable Long propertyId,
            @PathVariable Long amenityId) {
        amenityService.removeAmenityFromProperty(propertyId, amenityId);
        return ApiResponse.message("Amenity removed from property");
    }

    @GetMapping("/property/{propertyId}")
    @Operation(summary = "Get all amenities assigned to a property")
    public ApiResponse<List<AmenityResponse>> getPropertyAmenities(@PathVariable Long propertyId) {
        return ApiResponse.ok(amenityService.getPropertyAmenities(propertyId));
    }
}
