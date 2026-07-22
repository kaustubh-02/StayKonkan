package com.staykonkan.wishlist.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@Builder
public class WishlistResponse {

    private Long id;

    private Long propertyId;

    private String propertyTitle;

    private String propertyLocation;

    private BigDecimal pricePerNight;

    private BigDecimal averageRating;

    private String imageUrls;

    private Instant createdAt;
}
