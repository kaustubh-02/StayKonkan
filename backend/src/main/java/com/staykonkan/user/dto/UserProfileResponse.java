package com.staykonkan.user.dto;

import com.staykonkan.dto.BaseDTO;
import com.staykonkan.user.entity.Role;
import com.staykonkan.user.entity.UserStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class UserProfileResponse extends BaseDTO {

    private String fullName;
    private String email;
    private String phone;
    private Role role;
    private UserStatus status;
    private boolean emailVerified;
}
