package com.staykonkan.amenity.dto;

import com.staykonkan.amenity.entity.AmenityCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateAmenityRequest {

    @NotBlank(message = "Amenity name is required")
    @Size(max = 100, message = "Amenity name must not exceed 100 characters")
    private String name;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    @Size(max = 100, message = "Icon must not exceed 100 characters")
    private String icon;

    @NotNull(message = "Category is required")
    private AmenityCategory category;
}
