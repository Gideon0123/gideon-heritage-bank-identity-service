package com.gideonheritagebank.IDENTITY_SERVICE.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PUBLIC)
public class LoginRequest {

    @NotBlank(message = "Email address is required")
    @Email(message = "Please provide a valid email address")
    @Size(max = 254, message = "Email address must not exceed 254 characters")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(max = 128, message = "Password must not exceed 128 characters")
    private String password;
}
