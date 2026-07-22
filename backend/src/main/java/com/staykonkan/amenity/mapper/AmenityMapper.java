package com.staykonkan.amenity.mapper;

import com.staykonkan.amenity.dto.AmenityResponse;
import com.staykonkan.amenity.entity.Amenity;
import org.springframework.stereotype.Component;

@Component
public class AmenityMapper {

    public AmenityResponse toResponse(Amenity amenity) {

        return AmenityResponse.builder()
                .id(amenity.getId())
                .name(amenity.getName())
                .description(amenity.getDescription())
                .icon(amenity.getIcon())
                .category(amenity.getCategory())
                .active(amenity.getActive())
                .createdAt(amenity.getCreatedAt())
                .updatedAt(amenity.getUpdatedAt())
                .build();
    }
}
