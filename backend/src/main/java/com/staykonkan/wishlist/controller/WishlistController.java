package com.staykonkan.wishlist.controller;

import com.staykonkan.constant.AppConstants;
import com.staykonkan.dto.PageResponseDTO;
import com.staykonkan.response.ApiResponse;
import com.staykonkan.wishlist.dto.AddWishlistRequest;
import com.staykonkan.wishlist.dto.WishlistResponse;
import com.staykonkan.wishlist.service.WishlistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * Wishlist endpoints. All actions operate on the current authenticated
 * user (resolved from SecurityUserPrincipal inside the service layer) —
 * there is no cross-user access here, so no extra ownership checks are
 * needed at the controller level.
 */
@RestController
@RequestMapping(AppConstants.API_V1 + "/wishlist")
@Tag(name = "Wishlist", description = "Favorite properties for the current user")
public class WishlistController {

    private final WishlistService wishlistService;

    public WishlistController(WishlistService wishlistService) {
        this.wishlistService = wishlistService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add a property to the current user's wishlist")
    public ApiResponse<WishlistResponse> addToWishlist(@Valid @RequestBody AddWishlistRequest request) {
        return ApiResponse.ok(wishlistService.addToWishlist(request), "Property added to wishlist");
    }

    @DeleteMapping("/{propertyId}")
    @Operation(summary = "Remove a property from the current user's wishlist")
    public ApiResponse<Void> removeFromWishlist(@PathVariable Long propertyId) {
        wishlistService.removeFromWishlist(propertyId);
        return ApiResponse.message("Property removed from wishlist");
    }

    @GetMapping("/my")
    @Operation(summary = "Get the current user's wishlist")
    public ApiResponse<PageResponseDTO<WishlistResponse>> getMyWishlist(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(wishlistService.getMyWishlist(page, size));
    }

    @GetMapping("/check/{propertyId}")
    @Operation(summary = "Check whether a property is in the current user's wishlist")
    public ApiResponse<Boolean> isWishlisted(@PathVariable Long propertyId) {
        return ApiResponse.ok(wishlistService.isWishlisted(propertyId));
    }

    @GetMapping("/count/{propertyId}")
    @Operation(summary = "Get how many users have wishlisted a property")
    public ApiResponse<Long> getWishlistCount(@PathVariable Long propertyId) {
        return ApiResponse.ok(wishlistService.getWishlistCount(propertyId));
    }
}
