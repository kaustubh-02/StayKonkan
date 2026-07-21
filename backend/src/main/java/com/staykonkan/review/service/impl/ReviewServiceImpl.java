package com.staykonkan.review.service.impl;

import com.staykonkan.booking.entity.Booking;
import com.staykonkan.booking.entity.BookingStatus;
import com.staykonkan.booking.repository.BookingRepository;
import com.staykonkan.constant.AppConstants;
import com.staykonkan.dto.PageResponseDTO;
import com.staykonkan.exception.DuplicateResourceException;
import com.staykonkan.exception.ForbiddenException;
import com.staykonkan.exception.ResourceNotFoundException;
import com.staykonkan.exception.ValidationException;
import com.staykonkan.property.entity.Property;
import com.staykonkan.property.repository.PropertyRepository;
import com.staykonkan.review.dto.CreateReviewRequest;
import com.staykonkan.review.dto.OwnerReplyRequest;
import com.staykonkan.review.dto.RatingSummaryResponse;
import com.staykonkan.review.dto.ReviewResponse;
import com.staykonkan.review.dto.UpdateReviewRequest;
import com.staykonkan.review.entity.Review;
import com.staykonkan.review.mapper.ReviewMapper;
import com.staykonkan.review.repository.ReviewRepository;
import com.staykonkan.review.service.ReviewService;
import com.staykonkan.security.SecurityUserPrincipal;
import com.staykonkan.user.entity.Role;
import com.staykonkan.user.entity.User;
import com.staykonkan.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final PropertyRepository propertyRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final ReviewMapper reviewMapper;

    @Override
    public ReviewResponse createReview(CreateReviewRequest request) {

        User currentUser = getCurrentUser();

        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> ResourceNotFoundException.of("Booking", request.getBookingId()));

        if (!booking.getGuest().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("You can only review your own bookings");
        }

        if (booking.getStatus() != BookingStatus.COMPLETED) {
            throw new ValidationException("You can only review a booking after it has been completed");
        }

        if (reviewRepository.existsByBooking(booking)) {
            throw new DuplicateResourceException("A review already exists for this booking");
        }

        Property property = booking.getProperty();

        Review review = Review.builder()
                .property(property)
                .user(currentUser)
                .booking(booking)
                .rating(request.getRating())
                .title(request.getTitle())
                .comment(request.getComment())
                .build();

        review = reviewRepository.save(review);

        recalculateAverageRating(property);

        return reviewMapper.toResponse(review);
    }

    @Override
    public ReviewResponse updateReview(Long reviewId, UpdateReviewRequest request) {

        Review review = findByIdOrThrow(reviewId);
        User currentUser = getCurrentUser();

        if (!review.getUser().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("You can only edit your own review");
        }

        if (request.getRating() != null) {
            review.setRating(request.getRating());
        }
        if (request.getTitle() != null) {
            review.setTitle(request.getTitle());
        }
        if (request.getComment() != null) {
            review.setComment(request.getComment());
        }

        recalculateAverageRating(review.getProperty());

        return reviewMapper.toResponse(review);
    }

    @Override
    public void deleteReview(Long reviewId) {

        Review review = findByIdOrThrow(reviewId);
        User currentUser = getCurrentUser();

        boolean isAuthor = review.getUser().getId().equals(currentUser.getId());

        if (!isAuthor && currentUser.getRole() != Role.ADMIN) {
            throw new ForbiddenException("You do not have permission to delete this review");
        }

        Property property = review.getProperty();

        reviewRepository.delete(review);

        recalculateAverageRating(property);
    }

    @Override
    public ReviewResponse replyToReview(Long reviewId, OwnerReplyRequest request) {

        Review review = findByIdOrThrow(reviewId);
        User currentUser = getCurrentUser();

        boolean isPropertyOwner = review.getProperty().getOwner().getId().equals(currentUser.getId());

        if (!isPropertyOwner && currentUser.getRole() != Role.ADMIN) {
            throw new ForbiddenException("Only the property owner can reply to this review");
        }

        review.setOwnerReply(request.getReply());

        return reviewMapper.toResponse(review);
    }

    @Override
    public PageResponseDTO<ReviewResponse> getReviewsByProperty(Long propertyId, int page, int size, String sortBy) {

        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> ResourceNotFoundException.of("Property", propertyId));

        Pageable pageable = PageRequest.of(page, size, resolveSort(sortBy));

        Page<Review> reviewPage = reviewRepository.findByProperty(property, pageable);

        return PageResponseDTO.from(reviewPage.map(reviewMapper::toResponse));
    }

    @Override
    public PageResponseDTO<ReviewResponse> getMyReviews(int page, int size) {

        User currentUser = getCurrentUser();

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, AppConstants.DEFAULT_SORT_FIELD));

        Page<Review> reviewPage = reviewRepository.findByUser(currentUser, pageable);

        return PageResponseDTO.from(reviewPage.map(reviewMapper::toResponse));
    }

    @Override
    public List<ReviewResponse> getLatestReviews(Long propertyId, int limit) {

        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> ResourceNotFoundException.of("Property", propertyId));

        Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, AppConstants.DEFAULT_SORT_FIELD));

        return reviewRepository.findByProperty(property, pageable)
                .map(reviewMapper::toResponse)
                .getContent();
    }

    @Override
    public RatingSummaryResponse getRatingSummary(Long propertyId) {

        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> ResourceNotFoundException.of("Property", propertyId));

        return RatingSummaryResponse.builder()
                .propertyId(property.getId())
                .averageRating(property.getAverageRating())
                .totalReviews(property.getTotalReviews())
                .build();
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private Review findByIdOrThrow(Long reviewId) {
        return reviewRepository.findById(reviewId)
                .orElseThrow(() -> ResourceNotFoundException.of("Review", reviewId));
    }

    /**
     * Recomputes and persists Property.averageRating / totalReviews from the
     * Review table. Called after every create/update/delete so the property's
     * rating snapshot always reflects the current review set. An explicit
     * flush precedes the aggregate query so a review just saved/deleted in
     * this same transaction is guaranteed to be reflected in the result.
     */
    private void recalculateAverageRating(Property property) {
        reviewRepository.flush();

        Double avg = reviewRepository.findAverageRatingByProperty(property);
        long count = reviewRepository.countByProperty(property);

        property.setAverageRating(
                avg != null ? BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO
        );
        property.setTotalReviews((int) count);
    }

    private Sort resolveSort(String sortBy) {
        if (sortBy == null || sortBy.isBlank()) {
            return Sort.by(Sort.Direction.DESC, "createdAt");
        }
        return switch (sortBy.toLowerCase()) {
            case "newest" -> Sort.by(Sort.Direction.DESC, "createdAt");
            case "oldest" -> Sort.by(Sort.Direction.ASC, "createdAt");
            case "highest" -> Sort.by(Sort.Direction.DESC, "rating").and(Sort.by(Sort.Direction.DESC, "createdAt"));
            case "lowest" -> Sort.by(Sort.Direction.ASC, "rating").and(Sort.by(Sort.Direction.DESC, "createdAt"));
            default -> throw new ValidationException(
                    "Invalid sortBy value '" + sortBy + "'. Allowed values: newest, oldest, highest, lowest");
        };
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        SecurityUserPrincipal principal = (SecurityUserPrincipal) authentication.getPrincipal();
        return userRepository.findById(principal.getUserId())
                .orElseThrow(() -> ResourceNotFoundException.of("User", principal.getUserId()));
    }
}
