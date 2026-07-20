package com.staykonkan.user.controller;

import com.staykonkan.constant.AppConstants;
import com.staykonkan.dto.PageRequestDTO;
import com.staykonkan.dto.PageResponseDTO;
import com.staykonkan.response.ApiResponse;
import com.staykonkan.security.SecurityUserPrincipal;
import com.staykonkan.user.dto.UpdateProfileRequest;
import com.staykonkan.user.dto.UserProfileResponse;
import com.staykonkan.user.entity.Role;
import com.staykonkan.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Self-service profile endpoints (any authenticated user) plus
 * admin-only user CRUD, per the "User Module: Profile, Update Profile,
 * User CRUD" requirement. Broader admin dashboard/analytics endpoints
 * live in the Admin module instead — this controller stays focused on
 * the User entity itself.
 */
@RestController
@RequestMapping(AppConstants.API_V1 + "/users")
@Tag(name = "Users", description = "Profile management and user administration")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    @Operation(summary = "Get the current authenticated user's profile")
    public ApiResponse<UserProfileResponse> getMyProfile(@AuthenticationPrincipal SecurityUserPrincipal principal) {
        return ApiResponse.ok(userService.getProfile(principal.getUserId()));
    }

    @PutMapping("/me")
    @Operation(summary = "Update the current authenticated user's profile")
    public ApiResponse<UserProfileResponse> updateMyProfile(@AuthenticationPrincipal SecurityUserPrincipal principal,
                                                              @Valid @RequestBody UpdateProfileRequest request) {
        return ApiResponse.ok(userService.updateProfile(principal.getUserId(), request), "Profile updated successfully");
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List all users (Admin only)")
    public ApiResponse<PageResponseDTO<UserProfileResponse>> listUsers(
            @RequestParam(required = false) Role role,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageRequestDTO pageRequest = PageRequestDTO.builder().page(page).size(size).build();
        return ApiResponse.ok(userService.listUsers(pageRequest, role));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get any user by id (Admin only)")
    public ApiResponse<UserProfileResponse> getById(@PathVariable Long id) {
        return ApiResponse.ok(userService.getById(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Deactivate a user account (Admin only)")
    public ApiResponse<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ApiResponse.message("User deactivated successfully");
    }
}
