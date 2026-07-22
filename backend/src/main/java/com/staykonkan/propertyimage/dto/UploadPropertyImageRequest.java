package com.staykonkan.propertyimage.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
public class UploadPropertyImageRequest {

    @NotNull(message = "Property id is required")
    private Long propertyId;

    @NotNull(message = "Image file is required")
    private MultipartFile file;

    /**
     * Optional. When true, this image is set as the property's cover image
     * (demoting any previous cover). When null/false and this is the
     * property's first image, it still becomes cover automatically so a
     * property is never left without one.
     */
    private Boolean isCover;
}
