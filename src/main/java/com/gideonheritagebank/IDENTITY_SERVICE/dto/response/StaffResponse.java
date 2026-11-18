package com.gideonheritagebank.IDENTITY_SERVICE.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import com.gideonheritagebank.IDENTITY_SERVICE.enums.RoleName;
import com.gideonheritagebank.IDENTITY_SERVICE.enums.StaffIdentityStatus;
import com.gideonheritagebank.IDENTITY_SERVICE.enums.StaffRole;
import com.gideonheritagebank.IDENTITY_SERVICE.enums.UserType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffResponse {

    // Identity information
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

    // Employment information
    @Schema(description = "Employee number.", example = "GH-EMP-0001")
    private String employeeNumber;
    @Schema(description = "Staff role.")
    private StaffRole staffRole;
    @Schema(description = "Hire date.")
    private LocalDate hireDate;
    @Schema(description = "Last login at.")
    private Instant lastLoginAt;

    // Account information
    @Schema(description = "Enabled.")
    private boolean enabled;
    @Schema(description = "Email verified.")
    private boolean emailVerified;
    @Schema(description = "Account non locked.")
    private boolean accountNonLocked;
    @Schema(description = "Locked until.")
    private Instant lockedUntil;
    @Schema(description = "Suspended.")
    private boolean suspended;
    @Schema(description = "Identity closed.")
    private boolean identityClosed;
    @Schema(description = "Identity status.")
    private StaffIdentityStatus identityStatus;
    @Schema(description = "Roles.")
    private Set<RoleName> roles;

    // Audit information
    @Schema(description = "Created at.")
    private Instant createdAt;
    @Schema(description = "Updated at.")
    private Instant updatedAt;
}