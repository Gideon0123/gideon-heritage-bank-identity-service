package com.gideonheritagebank.IDENTITY_SERVICE.dto.response;

import com.gideonheritagebank.IDENTITY_SERVICE.enums.RoleName;
import com.gideonheritagebank.IDENTITY_SERVICE.enums.UserType;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class IdentityResponse {

    private UUID id;

    private String firstName;

    private String lastName;

    private String email;

    private String phoneNumber;

    private UserType userType;

    private boolean enabled;

    private boolean emailVerified;

    private Set<RoleName> roles;

    private Instant createdAt;
}
