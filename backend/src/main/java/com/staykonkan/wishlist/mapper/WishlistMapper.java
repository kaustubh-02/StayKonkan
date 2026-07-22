package com.staykonkan.wishlist.mapper;

import com.staykonkan.property.entity.Property;
import com.staykonkan.wishlist.dto.WishlistResponse;
import com.staykonkan.wishlist.entity.Wishlist;
import org.springframework.stereotype.Component;

@Component
public class WishlistMapper {

    public WishlistResponse toResponse(Wishlist wishlist) {

        WishlistResponse.WishlistResponseBuilder builder = WishlistResponse.builder()
                .id(wishlist.getId())
                .createdAt(wishlist.getCreatedAt());

        Property property = wishlist.getProperty();

        if (property != null) {
            builder.propertyId(property.getId())
                    .propertyTitle(property.getTitle())
                    .propertyLocation(property.getLocation())
                    .pricePerNight(property.getPricePerNight())
                    .averageRating(property.getAverageRating())
                    .imageUrls(property.getImageUrls());
        }

        return builder.build();
    }
}
