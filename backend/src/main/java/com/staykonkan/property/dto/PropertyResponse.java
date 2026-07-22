package com.staykonkan.property.dto;

import com.staykonkan.amenity.dto.AmenityResponse;
import com.staykonkan.property.entity.PropertyStatus;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Getter
@Setter
public class PropertyResponse {

    private Long id;

    private String title;

    private String description;

    private String location;

    private String address;

    private Double latitude;

    private Double longitude;

    private BigDecimal pricePerNight;

    private Integer maxGuests;

    private Integer bedrooms;

    private Integer bathrooms;

    private String amenities;

    // Structured, catalog-backed amenities (Module 8) — the legacy free-text
    // `amenities` field above is untouched for backward compatibility.
    private List<AmenityResponse> amenityDetails;

    private String imageUrls;

    private PropertyStatus propertyStatus;

    private Long ownerId;

    private String ownerName;

    private BigDecimal averageRating;

    private Integer totalReviews;

    private Instant createdAt;

    private Instant updatedAt;
}