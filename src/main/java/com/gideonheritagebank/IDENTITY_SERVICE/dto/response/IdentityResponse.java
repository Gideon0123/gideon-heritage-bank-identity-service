package com.gideonheritagebank.IDENTITY_SERVICE.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import com.gideonheritagebank.IDENTITY_SERVICE.enums.RoleName;
import com.gideonheritagebank.IDENTITY_SERVICE.enums.UserType;
import lombok.*;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class IdentityResponse {

    @Schema(description = "Id.")
    private UUID id;
    @Schema(description = "First name.", example = "Ada")
    private String firstName;
    @Schema(description = "Last name.", example = "Okafor")
    private String lastName;
    @Schema(description = "Email.", example = "customer@example.com")
    private String email;
    @Schema(description = "Phone number.", example = "+2348012345678")
    private String phoneNumber;
    @Schema(description = "User type.")
    private UserType userType;
    @Schema(description = "Enabled.")
    private boolean enabled;
    @Schema(description = "Email verified.")
    private boolean emailVerified;
    @Schema(description = "Roles.")
    private Set<RoleName> roles;
    @Schema(description = "Created at.")
    private Instant createdAt;
    @Schema(description = "Account non locked.")
    private boolean accountNonLocked;
    @Schema(description = "Profile photo url.", example = "https://cdn.example.com/profile.jpg")
    private String profilePhotoUrl;
    @Schema(description = "Locked until.")
    private Instant lockedUntil;
}
