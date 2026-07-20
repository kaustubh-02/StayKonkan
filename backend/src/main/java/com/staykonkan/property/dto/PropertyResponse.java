package com.staykonkan.property.dto;

import com.staykonkan.property.entity.PropertyStatus;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

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

    private String imageUrls;

    private PropertyStatus propertyStatus;

    private Long ownerId;

    private String ownerName;

    private Instant createdAt;

    private Instant updatedAt;
}