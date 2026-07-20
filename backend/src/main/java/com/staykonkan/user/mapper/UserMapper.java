package com.staykonkan.user.mapper;

import com.staykonkan.config.MapperConfig;
import com.staykonkan.user.dto.UserProfileResponse;
import com.staykonkan.user.entity.User;
import org.mapstruct.Mapper;

/**
 * Uses the foundation's shared MapperConfig (Phase 3, config/MapperConfig.java)
 * so every mapper in the codebase shares the same componentModel/injection/
 * unmapped-policy settings.
 */
@Mapper(config = MapperConfig.class)
public interface UserMapper {

    UserProfileResponse toProfileResponse(User user);
}
