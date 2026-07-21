package com.staykonkan.review.controller;

import com.staykonkan.constant.AppConstants;
import com.staykonkan.dto.PageResponseDTO;
import com.staykonkan.response.ApiResponse;
import com.staykonkan.review.dto.CreateReviewRequest;
import com.staykonkan.review.dto.OwnerReplyRequest;
import com.staykonkan.review.dto.RatingSummaryResponse;
import com.staykonkan.review.dto.ReviewResponse;
import com.staykonkan.review.dto.UpdateReviewRequest;
import com.staykonkan.review.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Review endpoints. As with Booking, per-record ownership checks (review
 * author, property owner, admin) live in ReviewServiceImpl since they
 * depend on data, not just the caller's role.
 */
@RestController
@RequestMapping(AppConstants.API_V1 + "/reviews")
@Tag(name = "Reviews", description = "Property reviews, ratings, and owner replies")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a review", description = "Only allowed for a COMPLETED booking belonging to the caller, and only once per booking")
    public ApiResponse<ReviewResponse> createReview(@Valid @RequestBody CreateReviewRequest request) {
        return ApiResponse.ok(reviewService.createReview(request), "Review created successfully");
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update your own review")
    public ApiResponse<ReviewResponse> updateReview(
            @PathVariable Long id,
            @Valid @RequestBody UpdateReviewRequest request) {
        return ApiResponse.ok(reviewService.updateReview(id, request), "Review updated successfully");
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a review", description = "Allowed for the review's author or an admin")
    public ApiResponse<Void> deleteReview(@PathVariable Long id) {
        reviewService.deleteReview(id);
        return ApiResponse.message("Review deleted successfully");
    }

    @PutMapping("/{id}/reply")
    @Operation(summary = "Reply to a review", description = "Allowed for the property owner or an admin. Does not modify the original review")
    public ApiResponse<ReviewResponse> replyToReview(
            @PathVariable Long id,
            @Valid @RequestBody OwnerReplyRequest request) {
        return ApiResponse.ok(reviewService.replyToReview(id, request), "Reply added successfully");
    }

    @GetMapping("/property/{propertyId}")
    @Operation(summary = "List reviews for a property", description = "Paginated and sortable by newest, oldest, highest, or lowest rating")
    public ApiResponse<PageResponseDTO<ReviewResponse>> getReviewsByProperty(
            @PathVariable Long propertyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "newest | oldest | highest | lowest")
            @RequestParam(defaultValue = "newest") String sortBy) {
        return ApiResponse.ok(reviewService.getReviewsByProperty(propertyId, page, size, sortBy));
    }

    @GetMapping("/property/{propertyId}/latest")
    @Operation(summary = "Get the most recent reviews for a property")
    public ApiResponse<List<ReviewResponse>> getLatestReviews(
            @PathVariable Long propertyId,
            @RequestParam(defaultValue = "5") int limit) {
        return ApiResponse.ok(reviewService.getLatestReviews(propertyId, limit));
    }

    @GetMapping("/property/{propertyId}/summary")
    @Operation(summary = "Get average rating and total review count for a property")
    public ApiResponse<RatingSummaryResponse> getRatingSummary(@PathVariable Long propertyId) {
        return ApiResponse.ok(reviewService.getRatingSummary(propertyId));
    }

    @GetMapping("/my")
    @Operation(summary = "List the current user's own reviews")
    public ApiResponse<PageResponseDTO<ReviewResponse>> getMyReviews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(reviewService.getMyReviews(page, size));
    }
}
