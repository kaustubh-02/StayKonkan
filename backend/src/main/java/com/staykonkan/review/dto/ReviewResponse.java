package com.staykonkan.review.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Builder
public class ReviewResponse {

    private Long id;

    private Long propertyId;

    private String propertyTitle;

    private Long userId;

    private String userName;

    private Long bookingId;

    private Integer rating;

    private String title;

    private String comment;

    private String ownerReply;

    private Instant createdAt;

    private Instant updatedAt;
}
