package com.staykonkan.property.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class UpdatePropertyRequest {

    private String title;

    private String description;

    private String location;

    private String address;

    private Double latitude;

    private Double longitude;

    @DecimalMin("0.0")
    private BigDecimal pricePerNight;

    @Min(1)
    private Integer maxGuests;

    @Min(1)
    private Integer bedrooms;

    @Min(1)
    private Integer bathrooms;

    private String amenities;

    private String imageUrls;
}