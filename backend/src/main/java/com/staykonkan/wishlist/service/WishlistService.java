package com.staykonkan.wishlist.service;

import com.staykonkan.dto.PageResponseDTO;
import com.staykonkan.wishlist.dto.AddWishlistRequest;
import com.staykonkan.wishlist.dto.WishlistResponse;

public interface WishlistService {

    WishlistResponse addToWishlist(AddWishlistRequest request);

    void removeFromWishlist(Long propertyId);

    PageResponseDTO<WishlistResponse> getMyWishlist(int page, int size);

    boolean isWishlisted(Long propertyId);

    long getWishlistCount(Long propertyId);
}
