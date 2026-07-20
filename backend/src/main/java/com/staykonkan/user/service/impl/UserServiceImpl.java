package com.staykonkan.user.service.impl;

import com.staykonkan.dto.PageRequestDTO;
import com.staykonkan.dto.PageResponseDTO;
import com.staykonkan.exception.ResourceNotFoundException;
import com.staykonkan.user.dto.UpdateProfileRequest;
import com.staykonkan.user.dto.UserProfileResponse;
import com.staykonkan.user.entity.Role;
import com.staykonkan.user.entity.User;
import com.staykonkan.user.entity.UserStatus;
import com.staykonkan.user.mapper.UserMapper;
import com.staykonkan.user.repository.UserRepository;
import com.staykonkan.user.service.UserService;
import com.staykonkan.util.PageUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public UserServiceImpl(UserRepository userRepository, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
    }

    @Override
    public UserProfileResponse getProfile(Long userId) {
        return userMapper.toProfileResponse(findByIdOrThrow(userId));
    }

    @Override
    @Transactional
    public UserProfileResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = findByIdOrThrow(userId);
        user.setFullName(request.getFullName());
        user.setPhone(request.getPhone());
        return userMapper.toProfileResponse(user);
        // No explicit save() call needed: `user` is a managed entity within
        // this @Transactional method, so Hibernate flushes the change via
        // dirty checking at commit — standard JPA pattern used consistently
        // across every module in this project.
    }

    @Override
    public PageResponseDTO<UserProfileResponse> listUsers(PageRequestDTO pageRequest, Role roleFilter) {
        Pageable pageable = PageUtils.toPageable(pageRequest);
        Page<User> page = (roleFilter != null)
                ? userRepository.findByRole(roleFilter, pageable)
                : userRepository.findAll(pageable);
        return PageResponseDTO.from(page.map(userMapper::toProfileResponse));
    }

    @Override
    public UserProfileResponse getById(Long userId) {
        return userMapper.toProfileResponse(findByIdOrThrow(userId));
    }

    @Override
    @Transactional
    public void deleteUser(Long userId) {
        User user = findByIdOrThrow(userId);
        // Soft delete — preserves referential integrity for the user's
        // existing properties/bookings/reviews (Phase 1 principle: never
        // hard-delete records with historical/financial significance).
        user.setStatus(UserStatus.DELETED);
    }

    private User findByIdOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> ResourceNotFoundException.of("User", userId));
    }
}
