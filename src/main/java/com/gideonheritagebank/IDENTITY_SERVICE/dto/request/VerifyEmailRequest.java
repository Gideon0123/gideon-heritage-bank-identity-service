package com.gideonheritagebank.IDENTITY_SERVICE.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PUBLIC)
public class VerifyEmailRequest {

    @NotBlank(message = "Verification token is required")
    @Size(max = 512, message = "Verification token is too long")
    private String token;
}
