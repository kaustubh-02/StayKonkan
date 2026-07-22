package com.staykonkan.amenity.dto;

import com.staykonkan.amenity.entity.AmenityCategory;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Builder
public class AmenityResponse {

    private Long id;

    private String name;

    private String description;

    private String icon;

    private AmenityCategory category;

    private Boolean active;

    private Instant createdAt;

    private Instant updatedAt;
}
