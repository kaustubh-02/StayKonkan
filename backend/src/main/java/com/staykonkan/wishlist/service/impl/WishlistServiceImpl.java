package com.staykonkan.wishlist.service.impl;

import com.staykonkan.constant.AppConstants;
import com.staykonkan.dto.PageResponseDTO;
import com.staykonkan.exception.DuplicateResourceException;
import com.staykonkan.exception.ForbiddenException;
import com.staykonkan.exception.ResourceNotFoundException;
import com.staykonkan.property.entity.Property;
import com.staykonkan.property.repository.PropertyRepository;
import com.staykonkan.security.SecurityUserPrincipal;
import com.staykonkan.user.entity.User;
import com.staykonkan.user.repository.UserRepository;
import com.staykonkan.wishlist.dto.AddWishlistRequest;
import com.staykonkan.wishlist.dto.WishlistResponse;
import com.staykonkan.wishlist.entity.Wishlist;
import com.staykonkan.wishlist.mapper.WishlistMapper;
import com.staykonkan.wishlist.repository.WishlistRepository;
import com.staykonkan.wishlist.service.WishlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class WishlistServiceImpl implements WishlistService {

    private final WishlistRepository wishlistRepository;
    private final PropertyRepository propertyRepository;
    private final UserRepository userRepository;
    private final WishlistMapper wishlistMapper;

    @Override
    public WishlistResponse addToWishlist(AddWishlistRequest request) {

        User currentUser = getCurrentUser();

        Property property = propertyRepository.findById(request.getPropertyId())
                .orElseThrow(() -> ResourceNotFoundException.of("Property", request.getPropertyId()));

        if (property.getOwner().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("You cannot add your own property to your wishlist");
        }

        if (wishlistRepository.existsByUserAndProperty(currentUser, property)) {
            throw new DuplicateResourceException("This property is already in your wishlist");
        }

        Wishlist wishlist = Wishlist.builder()
                .user(currentUser)
                .property(property)
                .build();

        wishlist = wishlistRepository.save(wishlist);

        return wishlistMapper.toResponse(wishlist);
    }

    @Override
    public void removeFromWishlist(Long propertyId) {

        User currentUser = getCurrentUser();

        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> ResourceNotFoundException.of("Property", propertyId));

        if (!wishlistRepository.existsByUserAndProperty(currentUser, property)) {
            throw new ResourceNotFoundException("This property is not in your wishlist");
        }

        wishlistRepository.deleteByUserAndProperty(currentUser, property);
    }

    @Override
    public PageResponseDTO<WishlistResponse> getMyWishlist(int page, int size) {

        User currentUser = getCurrentUser();

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, AppConstants.DEFAULT_SORT_FIELD));

        Page<Wishlist> wishlistPage = wishlistRepository.findByUser(currentUser, pageable);

        return PageResponseDTO.from(wishlistPage.map(wishlistMapper::toResponse));
    }

    @Override
    public boolean isWishlisted(Long propertyId) {

        User currentUser = getCurrentUser();

        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> ResourceNotFoundException.of("Property", propertyId));

        return wishlistRepository.existsByUserAndProperty(currentUser, property);
    }

    @Override
    public long getWishlistCount(Long propertyId) {

        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> ResourceNotFoundException.of("Property", propertyId));

        return wishlistRepository.countByProperty(property);
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        SecurityUserPrincipal principal = (SecurityUserPrincipal) authentication.getPrincipal();
        return userRepository.findById(principal.getUserId())
                .orElseThrow(() -> ResourceNotFoundException.of("User", principal.getUserId()));
    }
}
