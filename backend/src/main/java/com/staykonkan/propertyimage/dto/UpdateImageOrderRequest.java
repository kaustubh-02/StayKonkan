package com.staykonkan.propertyimage.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class UpdateImageOrderRequest {

    @NotEmpty(message = "At least one image order entry is required")
    @Valid
    private List<ImageOrderEntry> images;

    @Getter
    @Setter
    public static class ImageOrderEntry {

        @NotNull(message = "Image id is required")
        private Long imageId;

        @NotNull(message = "Display order is required")
        private Integer displayOrder;
    }
}
