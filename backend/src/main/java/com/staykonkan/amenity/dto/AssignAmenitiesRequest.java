package com.staykonkan.amenity.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
public class AssignAmenitiesRequest {

    @NotEmpty(message = "At least one amenity id is required")
    private Set<Long> amenityIds;
}
