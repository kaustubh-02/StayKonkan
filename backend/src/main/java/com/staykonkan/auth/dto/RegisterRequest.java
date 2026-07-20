package com.staykonkan.auth.dto;

import com.staykonkan.user.entity.Role;
import com.staykonkan.validation.StrongPassword;
import com.staykonkan.validation.ValidPhoneNumber;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    @NotBlank(message = "Full name is required")
    @Size(max = 150)
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid email address")
    private String email;

    @ValidPhoneNumber
    private String phone;

    @NotBlank(message = "Password is required")
    @StrongPassword
    private String password;

    /**
     * Optional — defaults to USER in AuthServiceImpl if omitted. Lets a
     * property owner self-register directly as OWNER. ADMIN is rejected
     * here regardless of what's sent (see AuthServiceImpl.register) —
     * admin accounts are never self-service.
     */
    private Role role;
}
