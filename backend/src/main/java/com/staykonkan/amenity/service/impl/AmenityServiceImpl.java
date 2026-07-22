package com.staykonkan.amenity.service.impl;

import com.staykonkan.amenity.dto.AmenityResponse;
import com.staykonkan.amenity.dto.AssignAmenitiesRequest;
import com.staykonkan.amenity.dto.CreateAmenityRequest;
import com.staykonkan.amenity.dto.UpdateAmenityRequest;
import com.staykonkan.amenity.entity.Amenity;
import com.staykonkan.amenity.entity.AmenityCategory;
import com.staykonkan.amenity.mapper.AmenityMapper;
import com.staykonkan.amenity.repository.AmenityRepository;
import com.staykonkan.amenity.service.AmenityService;
import com.staykonkan.exception.DuplicateResourceException;
import com.staykonkan.exception.ForbiddenException;
import com.staykonkan.exception.ResourceNotFoundException;
import com.staykonkan.exception.ValidationException;
import com.staykonkan.property.entity.Property;
import com.staykonkan.property.repository.PropertyRepository;
import com.staykonkan.security.SecurityUserPrincipal;
import com.staykonkan.user.entity.Role;
import com.staykonkan.user.entity.User;
import com.staykonkan.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * ADMIN-only actions (create/update/delete the catalog) are additionally
 * gated by @PreAuthorize("hasRole('ADMIN')") on the controller, matching
 * the UserController precedent. Per-property assignment actions depend on
 * data (who owns the property), so that check lives here, following the
 * same pattern used by Booking/Review/Wishlist/PropertyImage.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class AmenityServiceImpl implements AmenityService {

    private final AmenityRepository amenityRepository;
    private final PropertyRepository propertyRepository;
    private final UserRepository userRepository;
    private final AmenityMapper amenityMapper;

    @Override
    public AmenityResponse createAmenity(CreateAmenityRequest request) {

        String normalizedName = request.getName().trim();

        if (amenityRepository.existsByName(normalizedName)) {
            throw new DuplicateResourceException("An amenity named '" + normalizedName + "' already exists");
        }

        Amenity amenity = Amenity.builder()
                .name(normalizedName)
                .description(request.getDescription())
                .icon(request.getIcon())
                .category(request.getCategory())
                .active(true)
                .build();

        amenity = amenityRepository.save(amenity);

        return amenityMapper.toResponse(amenity);
    }

    @Override
    public AmenityResponse updateAmenity(Long amenityId, UpdateAmenityRequest request) {

        Amenity amenity = findByIdOrThrow(amenityId);

        if (request.getName() != null) {
            String normalizedName = request.getName().trim();
            amenityRepository.findByName(normalizedName).ifPresent(existing -> {
                if (!existing.getId().equals(amenityId)) {
                    throw new DuplicateResourceException("An amenity named '" + normalizedName + "' already exists");
                }
            });
            amenity.setName(normalizedName);
        }
        if (request.getDescription() != null) {
            amenity.setDescription(request.getDescription());
        }
        if (request.getIcon() != null) {
            amenity.setIcon(request.getIcon());
        }
        if (request.getCategory() != null) {
            amenity.setCategory(request.getCategory());
        }
        if (request.getActive() != null) {
            amenity.setActive(request.getActive());
        }

        return amenityMapper.toResponse(amenity);
    }

    @Override
    public void deleteAmenity(Long amenityId) {

        Amenity amenity = findByIdOrThrow(amenityId);

        // Soft delete: an amenity already assigned to existing properties
        // must not be hard-removed (would silently strip it from every
        // property's amenity list and violate the M2M join row history).
        // It's simply hidden from future selection.
        amenity.setActive(false);
    }

    @Override
    public List<AmenityResponse> getAllAmenities() {
        return amenityRepository.findAll()
                .stream()
                .map(amenityMapper::toResponse)
                .toList();
    }

    @Override
    public List<AmenityResponse> getAmenitiesByCategory(AmenityCategory category) {
        return amenityRepository.findByCategory(category)
                .stream()
                .map(amenityMapper::toResponse)
                .toList();
    }

    @Override
    public List<AmenityResponse> assignAmenitiesToProperty(Long propertyId, AssignAmenitiesRequest request) {

        User currentUser = getCurrentUser();
        Property property = findPropertyOrThrow(propertyId);

        assertOwnerOrAdmin(property, currentUser);

        Set<Long> requestedIds = request.getAmenityIds();

        List<Amenity> amenities = amenityRepository.findAllById(requestedIds);

        if (amenities.size() != requestedIds.size()) {
            throw new ResourceNotFoundException("One or more amenities were not found");
        }

        boolean hasInactive = amenities.stream().anyMatch(a -> !Boolean.TRUE.equals(a.getActive()));
        if (hasInactive) {
            throw new ValidationException("One or more amenities are inactive and cannot be assigned");
        }

        Set<Amenity> currentAmenities = property.getAmenityList();
        if (currentAmenities == null) {
            currentAmenities = new HashSet<>();
            property.setAmenityList(currentAmenities);
        }

        // existsByName-style duplicate prevention: skip anything already
        // assigned rather than erroring, so a client can safely re-submit
        // the same assignment call idempotently.
        currentAmenities.addAll(amenities);

        return property.getAmenityList()
                .stream()
                .map(amenityMapper::toResponse)
                .toList();
    }

    @Override
    public void removeAmenityFromProperty(Long propertyId, Long amenityId) {

        User currentUser = getCurrentUser();
        Property property = findPropertyOrThrow(propertyId);

        assertOwnerOrAdmin(property, currentUser);

        Amenity amenity = findByIdOrThrow(amenityId);

        if (property.getAmenityList() == null || !property.getAmenityList().remove(amenity)) {
            throw new ResourceNotFoundException("This amenity is not assigned to the property");
        }
    }

    @Override
    public List<AmenityResponse> getPropertyAmenities(Long propertyId) {

        Property property = findPropertyOrThrow(propertyId);

        if (property.getAmenityList() == null) {
            return List.of();
        }

        return property.getAmenityList()
                .stream()
                .map(amenityMapper::toResponse)
                .toList();
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private Amenity findByIdOrThrow(Long amenityId) {
        return amenityRepository.findById(amenityId)
                .orElseThrow(() -> ResourceNotFoundException.of("Amenity", amenityId));
    }

    private Property findPropertyOrThrow(Long propertyId) {
        return propertyRepository.findById(propertyId)
                .orElseThrow(() -> ResourceNotFoundException.of("Property", propertyId));
    }

    private void assertOwnerOrAdmin(Property property, User currentUser) {
        boolean isOwner = property.getOwner().getId().equals(currentUser.getId());
        if (!isOwner && currentUser.getRole() != Role.ADMIN) {
            throw new ForbiddenException("Only the property owner or an admin can manage amenities for this property");
        }
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        SecurityUserPrincipal principal = (SecurityUserPrincipal) authentication.getPrincipal();
        return userRepository.findById(principal.getUserId())
                .orElseThrow(() -> ResourceNotFoundException.of("User", principal.getUserId()));
    }
}
