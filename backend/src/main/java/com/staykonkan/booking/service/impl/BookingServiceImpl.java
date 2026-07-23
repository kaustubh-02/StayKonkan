package com.staykonkan.booking.service.impl;

import com.staykonkan.availability.service.PropertyAvailabilityService;
import com.staykonkan.booking.dto.BookingResponse;
import com.staykonkan.booking.dto.CreateBookingRequest;
import com.staykonkan.booking.dto.UpdateBookingStatusRequest;
import com.staykonkan.booking.mapper.BookingMapper;
import com.staykonkan.booking.repository.BookingRepository;
import com.staykonkan.booking.service.BookingService;
import com.staykonkan.constant.AppConstants;
import com.staykonkan.dto.PageResponseDTO;
import com.staykonkan.exception.ForbiddenException;
import com.staykonkan.exception.InvalidStateTransitionException;
import com.staykonkan.exception.ResourceNotFoundException;
import com.staykonkan.exception.ValidationException;
import com.staykonkan.property.repository.PropertyRepository;
import com.staykonkan.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import com.staykonkan.booking.entity.Booking;
import com.staykonkan.booking.entity.BookingStatus;
import com.staykonkan.property.entity.Property;
import com.staykonkan.security.SecurityUserPrincipal;
import com.staykonkan.user.entity.Role;
import com.staykonkan.user.entity.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final PropertyRepository propertyRepository;
    private final UserRepository userRepository;
    private final BookingMapper bookingMapper;
    private final PropertyAvailabilityService availabilityService;

    @Override
    public BookingResponse createBooking(CreateBookingRequest request) {

        User guest = getCurrentUser();

        Property property = propertyRepository.findById(request.getPropertyId())
                .orElseThrow(() -> ResourceNotFoundException.of("Property", request.getPropertyId()));

        LocalDate checkInDate = request.getCheckInDate();
        LocalDate checkOutDate = request.getCheckOutDate();

        if (!checkOutDate.isAfter(checkInDate)) {
            throw new ValidationException("Check-out date must be after check-in date");
        }

        if (property.getOwner().getId().equals(guest.getId())) {
            throw new ValidationException("You cannot book your own property");
        }

        if (request.getGuestCount() > property.getMaxGuests()) {
            throw new ValidationException(
                    "Guest count exceeds the maximum allowed for this property (" + property.getMaxGuests() + ")");
        }

        boolean overlapping = bookingRepository
                .existsByPropertyAndStatusInAndCheckInDateLessThanAndCheckOutDateGreaterThan(
                        property,
                        List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED),
                        checkOutDate,
                        checkInDate
                );

        if (overlapping) {
            throw new ValidationException("Property is already booked for the selected dates");
        }

        // Second, independent guard: existing overlap check above only
        // looks at other Bookings (PENDING/CONFIRMED). This also rejects
        // dates the owner has explicitly BLOCKED or marked MAINTENANCE
        // via the availability calendar (Module 9), which the Booking
        // table alone has no knowledge of. The authoritative,
        // race-condition-safe reservation still happens in
        // availabilityService.bookDates() at confirmation time below.
        if (!availabilityService.checkAvailability(property.getId(), checkInDate, checkOutDate)) {
            throw new ValidationException(
                    "Selected dates are not available (blocked by the host or under maintenance)");
        }

        long nights = ChronoUnit.DAYS.between(checkInDate, checkOutDate);
        BigDecimal totalAmount = property.getPricePerNight().multiply(BigDecimal.valueOf(nights));

        Booking booking = Booking.builder()
                .bookingCode(generateBookingCode())
                .property(property)
                .guest(guest)
                .checkInDate(checkInDate)
                .checkOutDate(checkOutDate)
                .guestCount(request.getGuestCount())
                .pricePerNight(property.getPricePerNight())
                .totalAmount(totalAmount)
                .specialRequests(request.getSpecialRequests())
                .build();

        booking = bookingRepository.save(booking);

        return bookingMapper.toResponse(booking);
    }

    @Override
    public BookingResponse getBookingById(Long bookingId) {

        Booking booking = findByIdOrThrow(bookingId);

        User currentUser = getCurrentUser();

        if (!canAccessBooking(currentUser, booking)) {
            throw new ForbiddenException("You do not have permission to view this booking");
        }

        return bookingMapper.toResponse(booking);
    }

    @Override
    public PageResponseDTO<BookingResponse> getMyBookings(int page, int size) {

        User guest = getCurrentUser();

        Pageable pageable = defaultPageable(page, size);

        Page<Booking> bookingPage = bookingRepository.findByGuest(guest, pageable);

        return PageResponseDTO.from(bookingPage.map(bookingMapper::toResponse));
    }

    @Override
    public PageResponseDTO<BookingResponse> getOwnerBookings(int page, int size) {

        User owner = getCurrentUser();

        Pageable pageable = defaultPageable(page, size);

        Page<Booking> bookingPage = bookingRepository.findByPropertyOwner(owner, pageable);

        return PageResponseDTO.from(bookingPage.map(bookingMapper::toResponse));
    }

    @Override
    public void cancelBooking(Long bookingId) {

        Booking booking = findByIdOrThrow(bookingId);

        User currentUser = getCurrentUser();

        boolean isGuest = booking.getGuest().getId().equals(currentUser.getId());
        boolean isPropertyOwner = booking.getProperty().getOwner().getId().equals(currentUser.getId());

        if (!isGuest && !isPropertyOwner && currentUser.getRole() != Role.ADMIN) {
            throw new ForbiddenException("You do not have permission to cancel this booking");
        }

        validateStatusTransition(booking.getStatus(), BookingStatus.CANCELLED);

        // Safe even if this booking was never CONFIRMED (PENDING
        // cancellations are a no-op here — see releaseBookingDates doc).
        availabilityService.releaseBookingDates(booking.getId());

        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancelledAt(LocalDateTime.now());
    }

    @Override
    public BookingResponse updateBookingStatus(Long bookingId, UpdateBookingStatusRequest request) {

        Booking booking = findByIdOrThrow(bookingId);

        User currentUser = getCurrentUser();

        boolean isPropertyOwner = booking.getProperty().getOwner().getId().equals(currentUser.getId());

        if (!isPropertyOwner && currentUser.getRole() != Role.ADMIN) {
            throw new ForbiddenException("Only the property owner can update the booking status");
        }

        BookingStatus targetStatus = request.getStatus();

        validateStatusTransition(booking.getStatus(), targetStatus);

        // Mutate the authoritative availability calendar BEFORE flipping
        // the booking's own status, so that if bookDates() throws (dates
        // no longer available — e.g. a race with another confirmation,
        // or the host blocked the dates after this booking was created),
        // the whole transaction rolls back and the booking stays in its
        // current status rather than ending up CONFIRMED with no
        // corresponding BOOKED calendar rows.
        if (targetStatus == BookingStatus.CONFIRMED) {
            availabilityService.bookDates(
                    booking.getProperty().getId(), booking.getCheckInDate(), booking.getCheckOutDate(), booking.getId());
        } else if (targetStatus == BookingStatus.CANCELLED) {
            availabilityService.releaseBookingDates(booking.getId());
        }

        booking.setStatus(targetStatus);

        switch (targetStatus) {
            case CONFIRMED -> booking.setConfirmedAt(LocalDateTime.now());
            case CANCELLED -> booking.setCancelledAt(LocalDateTime.now());
            case COMPLETED -> booking.setCompletedAt(LocalDateTime.now());
            default -> {
                // PENDING has no timestamp of its own to set.
            }
        }

        return bookingMapper.toResponse(booking);
    }

    @Override
    public BookingResponse confirmAfterPayment(Long bookingId) {

        Booking booking = findByIdOrThrow(bookingId);

        validateStatusTransition(booking.getStatus(), BookingStatus.CONFIRMED);

        // Same authoritative, race-condition-safe reservation used by the
        // owner/admin manual-confirm path in updateBookingStatus — see
        // that method's comment for why this happens before the status
        // flip, and PropertyAvailabilityServiceImpl.bookDates for the
        // pessimistic-lock mechanics.
        availabilityService.bookDates(
                booking.getProperty().getId(), booking.getCheckInDate(), booking.getCheckOutDate(), booking.getId());

        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setConfirmedAt(LocalDateTime.now());

        return bookingMapper.toResponse(booking);
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private Booking findByIdOrThrow(Long bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> ResourceNotFoundException.of("Booking", bookingId));
    }

    private boolean canAccessBooking(User currentUser, Booking booking) {
        boolean isGuest = booking.getGuest().getId().equals(currentUser.getId());
        boolean isPropertyOwner = booking.getProperty().getOwner().getId().equals(currentUser.getId());
        return isGuest || isPropertyOwner || currentUser.getRole() == Role.ADMIN;
    }

    private Pageable defaultPageable(int page, int size) {
        Sort sort = Sort.by(
                AppConstants.DEFAULT_SORT_DIRECTION.equalsIgnoreCase("ASC")
                        ? Sort.Direction.ASC
                        : Sort.Direction.DESC,
                AppConstants.DEFAULT_SORT_FIELD
        );
        return PageRequest.of(page, size, sort);
    }

    /**
     * Enforces the Booking state machine: PENDING -> CONFIRMED/CANCELLED,
     * CONFIRMED -> COMPLETED/CANCELLED. CANCELLED and COMPLETED are
     * terminal states. Violations throw InvalidStateTransitionException
     * (409), per the exception declared for exactly this purpose.
     */
    private void validateStatusTransition(BookingStatus current, BookingStatus target) {

        boolean valid = switch (current) {
            case PENDING -> target == BookingStatus.CONFIRMED || target == BookingStatus.CANCELLED;
            case CONFIRMED -> target == BookingStatus.COMPLETED || target == BookingStatus.CANCELLED;
            case CANCELLED, COMPLETED -> false;
        };

        if (!valid) {
            throw new InvalidStateTransitionException(
                    "Cannot change booking status from " + current + " to " + target);
        }
    }

    private String generateBookingCode() {
        return "BK-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        SecurityUserPrincipal principal = (SecurityUserPrincipal) authentication.getPrincipal();
        return userRepository.findById(principal.getUserId())
                .orElseThrow(() -> ResourceNotFoundException.of("User", principal.getUserId()));
    }
}
