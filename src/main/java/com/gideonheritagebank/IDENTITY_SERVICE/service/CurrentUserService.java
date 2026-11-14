package com.gideonheritagebank.IDENTITY_SERVICE.service;

import com.gideonheritagebank.IDENTITY_SERVICE.entity.CurrentUser;
import com.gideonheritagebank.IDENTITY_SERVICE.exception.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CurrentUserService {

    public CurrentUser getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext()
                .getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw new AccessDeniedException("An authenticated JWT principal is required.");
        }

        String subject = jwt.getSubject();
        if (subject == null || subject.isBlank()) {
            throw new AccessDeniedException("Authenticated user identifier is missing.");
        }

        final UUID userId;
        try {
            userId = UUID.fromString(subject);
        } catch (IllegalArgumentException exception) {
            throw new AccessDeniedException("Authenticated user identifier is invalid.");
        }

        Object rolesClaim = jwt.getClaims().get("roles");
        String roles = rolesClaim instanceof Collection<?> values
                ? values.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .sorted()
                .collect(Collectors.joining(","))
                : null;

        return CurrentUser.builder()
                .id(userId)
                .email(jwt.getClaimAsString("email"))
                .role(roles)
                .build();
    }
}
