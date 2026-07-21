package com.staykonkan.review.service;

import com.staykonkan.dto.PageResponseDTO;
import com.staykonkan.review.dto.CreateReviewRequest;
import com.staykonkan.review.dto.OwnerReplyRequest;
import com.staykonkan.review.dto.RatingSummaryResponse;
import com.staykonkan.review.dto.ReviewResponse;
import com.staykonkan.review.dto.UpdateReviewRequest;

import java.util.List;

public interface ReviewService {

    ReviewResponse createReview(CreateReviewRequest request);

    ReviewResponse updateReview(Long reviewId, UpdateReviewRequest request);

    void deleteReview(Long reviewId);

    ReviewResponse replyToReview(Long reviewId, OwnerReplyRequest request);

    PageResponseDTO<ReviewResponse> getReviewsByProperty(Long propertyId, int page, int size, String sortBy);

    PageResponseDTO<ReviewResponse> getMyReviews(int page, int size);

    List<ReviewResponse> getLatestReviews(Long propertyId, int limit);

    RatingSummaryResponse getRatingSummary(Long propertyId);
}
