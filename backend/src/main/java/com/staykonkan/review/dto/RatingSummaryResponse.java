package com.staykonkan.review.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
public class RatingSummaryResponse {

    private Long propertyId;

    private BigDecimal averageRating;

    private Integer totalReviews;
}
