package com.gideonheritagebank.IDENTITY_SERVICE.dto.response;

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
    private UUID id;
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private UserType userType;

    // Employment information
    private String employeeNumber;
    private StaffRole staffRole;
    private LocalDate hireDate;
    private Instant lastLoginAt;

    // Account information
    private boolean enabled;
    private boolean emailVerified;
    private boolean accountNonLocked;
    private Instant lockedUntil;
    private boolean suspended;
    private boolean identityClosed;
    private StaffIdentityStatus identityStatus;
    private Set<RoleName> roles;

    // Audit information
    private Instant createdAt;
    private Instant updatedAt;
}