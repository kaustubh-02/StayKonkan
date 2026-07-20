package com.staykonkan.user.service;

import com.staykonkan.dto.PageRequestDTO;
import com.staykonkan.dto.PageResponseDTO;
import com.staykonkan.user.dto.UpdateProfileRequest;
import com.staykonkan.user.dto.UserProfileResponse;
import com.staykonkan.user.entity.Role;

public interface UserService {

    UserProfileResponse getProfile(Long userId);

    UserProfileResponse updateProfile(Long userId, UpdateProfileRequest request);

    PageResponseDTO<UserProfileResponse> listUsers(PageRequestDTO pageRequest, Role roleFilter);

    UserProfileResponse getById(Long userId);

    void deleteUser(Long userId);
}
