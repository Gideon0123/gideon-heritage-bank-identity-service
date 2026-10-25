package com.gideonheritagebank.IDENTITY_SERVICE.service;

import com.gideonheritagebank.IDENTITY_SERVICE.dto.request.RegisterRequest;
import com.gideonheritagebank.IDENTITY_SERVICE.dto.response.IdentityResponse;
import com.gideonheritagebank.IDENTITY_SERVICE.entity.BankUser;
import com.gideonheritagebank.IDENTITY_SERVICE.entity.Role;
import com.gideonheritagebank.IDENTITY_SERVICE.enums.RoleName;
import com.gideonheritagebank.IDENTITY_SERVICE.exception.EmailAlreadyRegisteredException;
import com.gideonheritagebank.IDENTITY_SERVICE.exception.InvalidRegistrationException;
import com.gideonheritagebank.IDENTITY_SERVICE.exception.PhoneNumberAlreadyRegisteredException;
import com.gideonheritagebank.IDENTITY_SERVICE.exception.RoleConfigurationException;
import com.gideonheritagebank.IDENTITY_SERVICE.repository.BankUserRepository;
import com.gideonheritagebank.IDENTITY_SERVICE.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class IdentityService {

    private final BankUserRepository bankUserRepository;

    private final RoleRepository roleRepository;

    private final PasswordEncoder passwordEncoder;

    @Transactional
    public IdentityResponse registerCustomer(RegisterRequest request) {

        validatePasswordConfirmation(request);

        String firstName = normalizeName(request.getFirstName());
        String lastName = normalizeName(request.getLastName());
        String email = normalizeEmail(request.getEmail());
        String phoneNumber = normalizePhoneNumber(request.getPhoneNumber());

        validateRegistrationFields(
                firstName,
                lastName,
                email,
                phoneNumber
        );

        validateEmailUniqueness(email);
        validatePhoneNumberUniqueness(phoneNumber);

        Role customerRole = roleRepository.findByName(RoleName.CUSTOMER)
                .orElseThrow(() -> new RoleConfigurationException(
                        "Required CUSTOMER role is missing from the database"
                ));

        String passwordHash = passwordEncoder.encode(request.getPassword());

        BankUser user = BankUser.createCustomer(
                firstName,
                lastName,
                email,
                phoneNumber,
                passwordHash
        );

        user.addRole(customerRole);

        BankUser savedUser = bankUserRepository.saveAndFlush(user);

        return toIdentityResponse(savedUser);
    }

    private void validatePasswordConfirmation(RegisterRequest request) {

        if (request.getPassword() == null
                || request.getConfirmPassword() == null
                || !request.getPassword().equals(request.getConfirmPassword())) {

            throw new InvalidRegistrationException(
                    "Password and confirmation password must match"
            );
        }
    }

    private void validateRegistrationFields(
            String firstName,
            String lastName,
            String email,
            String phoneNumber
    ) {
        if (firstName.isBlank()) {
            throw new InvalidRegistrationException(
                    "First name is required"
            );
        }

        if (lastName.isBlank()) {
            throw new InvalidRegistrationException(
                    "Last name is required"
            );
        }

        if (email.isBlank()) {
            throw new InvalidRegistrationException(
                    "Email address is required"
            );
        }

        if (phoneNumber.isBlank()) {
            throw new InvalidRegistrationException(
                    "Phone number is required"
            );
        }
    }

    private void validateEmailUniqueness(String email) {

        if (bankUserRepository.existsByEmail(email)) {
            throw new EmailAlreadyRegisteredException();
        }
    }

    private void validatePhoneNumberUniqueness(String phoneNumber) {

        if (bankUserRepository.existsByPhoneNumber(phoneNumber)) {
            throw new PhoneNumberAlreadyRegisteredException();
        }
    }

    private String normalizeName(String value) {

        if (value == null) {
            return "";
        }

        return value.trim().replaceAll("\\s+", " ");
    }

    private String normalizeEmail(String email) {

        if (email == null) {
            return "";
        }

        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizePhoneNumber(String phoneNumber) {

        if (phoneNumber == null) {
            return "";
        }

        return phoneNumber.trim();
    }

    private IdentityResponse toIdentityResponse(BankUser user) {

        Set<RoleName> roles = user.getRoles()
                .stream()
                .map(Role::getName)
                .collect(Collectors.toUnmodifiableSet());

        return IdentityResponse.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .userType(user.getUserType())
                .enabled(user.isEnabled())
                .emailVerified(user.isEmailVerified())
                .roles(roles)
                .createdAt(user.getCreatedAt())
                .build();
    }
}
