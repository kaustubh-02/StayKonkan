package com.staykonkan.propertyimage.mapper;

import com.staykonkan.propertyimage.dto.PropertyImageResponse;
import com.staykonkan.propertyimage.entity.PropertyImage;
import org.springframework.stereotype.Component;

@Component
public class PropertyImageMapper {

    public PropertyImageResponse toResponse(PropertyImage image) {

        PropertyImageResponse.PropertyImageResponseBuilder builder = PropertyImageResponse.builder()
                .id(image.getId())
                .imageUrl(image.getImageUrl())
                .publicId(image.getPublicId())
                .isCover(image.getIsCover())
                .displayOrder(image.getDisplayOrder())
                .createdAt(image.getCreatedAt())
                .updatedAt(image.getUpdatedAt());

        if (image.getProperty() != null) {
            builder.propertyId(image.getProperty().getId());
        }

        return builder.build();
    }
}
