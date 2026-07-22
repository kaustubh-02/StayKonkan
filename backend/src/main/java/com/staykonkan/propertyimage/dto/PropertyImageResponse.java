package com.staykonkan.propertyimage.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Builder
public class PropertyImageResponse {

    private Long id;

    private Long propertyId;

    private String imageUrl;

    private String publicId;

    private Boolean isCover;

    private Integer displayOrder;

    private Instant createdAt;

    private Instant updatedAt;
}
