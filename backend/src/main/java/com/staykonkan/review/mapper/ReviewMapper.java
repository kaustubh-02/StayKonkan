package com.staykonkan.review.mapper;

import com.staykonkan.review.dto.ReviewResponse;
import com.staykonkan.review.entity.Review;
import org.springframework.stereotype.Component;

@Component
public class ReviewMapper {

    public ReviewResponse toResponse(Review review) {

        ReviewResponse.ReviewResponseBuilder builder = ReviewResponse.builder()
                .id(review.getId())
                .rating(review.getRating())
                .title(review.getTitle())
                .comment(review.getComment())
                .ownerReply(review.getOwnerReply())
                .createdAt(review.getCreatedAt())
                .updatedAt(review.getUpdatedAt());

        if (review.getProperty() != null) {
            builder.propertyId(review.getProperty().getId());
            builder.propertyTitle(review.getProperty().getTitle());
        }

        if (review.getUser() != null) {
            builder.userId(review.getUser().getId());
            builder.userName(review.getUser().getFullName());
        }

        if (review.getBooking() != null) {
            builder.bookingId(review.getBooking().getId());
        }

        return builder.build();
    }
}
