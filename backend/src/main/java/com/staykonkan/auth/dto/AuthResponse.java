package com.staykonkan.auth.dto;

import com.staykonkan.user.dto.UserProfileResponse;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuthResponse {

    private String accessToken;
    private String refreshToken;

    @Builder.Default
    private String tokenType = "Bearer";

    private long expiresInSeconds;
    private UserProfileResponse user;
}
