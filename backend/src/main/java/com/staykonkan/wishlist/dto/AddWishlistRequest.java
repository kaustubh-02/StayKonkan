package com.staykonkan.wishlist.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AddWishlistRequest {

    @NotNull(message = "Property id is required")
    private Long propertyId;
}
