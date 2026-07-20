package com.staykonkan.property.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class CreatePropertyRequest {

    @NotBlank
    private String title;

    @NotBlank
    private String description;

    @NotBlank
    private String location;

    @NotBlank
    private String address;

    private Double latitude;

    private Double longitude;

    @NotNull
    @DecimalMin("0.0")
    private BigDecimal pricePerNight;

    @NotNull
    @Min(1)
    private Integer maxGuests;

    @NotNull
    @Min(1)
    private Integer bedrooms;

    @NotNull
    @Min(1)
    private Integer bathrooms;

    private String amenities;

    private String imageUrls;
}