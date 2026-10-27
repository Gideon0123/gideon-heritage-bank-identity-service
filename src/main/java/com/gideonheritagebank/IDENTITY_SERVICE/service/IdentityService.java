package com.gideonheritagebank.IDENTITY_SERVICE.service;

import com.gideonheritagebank.IDENTITY_SERVICE.dto.request.RegisterRequest;
import com.gideonheritagebank.IDENTITY_SERVICE.dto.response.IdentityResponse;
import com.gideonheritagebank.IDENTITY_SERVICE.entity.BankUser;
import com.gideonheritagebank.IDENTITY_SERVICE.entity.Role;
import com.gideonheritagebank.IDENTITY_SERVICE.enums.RoleName;
import org.springframework.dao.DataIntegrityViolationException;
import org.hibernate.exception.ConstraintViolationException;
import com.gideonheritagebank.IDENTITY_SERVICE.exception.EmailAlreadyRegisteredException;
import com.gideonheritagebank.IDENTITY_SERVICE.exception.InvalidRegistrationException;
import com.gideonheritagebank.IDENTITY_SERVICE.exception.PhoneNumberAlreadyRegisteredException;
import com.gideonheritagebank.IDENTITY_SERVICE.exception.RoleConfigurationException;
import com.gideonheritagebank.IDENTITY_SERVICE.exception.IdentityNotFoundException;
import com.gideonheritagebank.IDENTITY_SERVICE.exception.InvalidIdentityStateException;
import com.gideonheritagebank.IDENTITY_SERVICE.mapper.IdentityMapper;
import com.gideonheritagebank.IDENTITY_SERVICE.repository.BankUserRepository;
import com.gideonheritagebank.IDENTITY_SERVICE.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class IdentityService {

    private final BankUserRepository bankUserRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final IdentityMapper identityMapper;

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

        /*
         * saveAndFlush forces pending SQL operations to be sent to the
         * database before this method returns. This allows unique
         * constraint violations to surface within this method's execution.
         *
         * The database unique constraints remain essential because two
         * concurrent registrations can both pass the preliminary checks.
         */
        BankUser savedUser;

        try {
            savedUser = bankUserRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException exception) {
            throw translateRegistrationConflict(exception);
        }

        return identityMapper.toIdentityResponse(savedUser);
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

    private RuntimeException translateRegistrationConflict(
            DataIntegrityViolationException exception
    ) {
        String constraintName = findConstraintName(exception);

        if (constraintName != null) {
            String normalizedConstraint = constraintName.toLowerCase(Locale.ROOT);

            if (normalizedConstraint.contains("email")) {
                return new EmailAlreadyRegisteredException();
            }

            if (normalizedConstraint.contains("phone_number")
                    || normalizedConstraint.contains("phone")) {
                return new PhoneNumberAlreadyRegisteredException();
            }
        }

        // Unknown integrity failures must not be misreported as duplicate emails
        // or phone numbers. Let the existing global handler handle the conflict.
        return exception;
    }

    private String findConstraintName(Throwable exception) {
        Throwable current = exception;

        while (current != null) {
            if (current instanceof ConstraintViolationException violation) {
                return violation.getConstraintName();
            }

            current = current.getCause();
        }

        return null;
    }

    @Transactional
    public IdentityResponse enableIdentity(UUID userId) {
        BankUser user = getIdentityForUpdate(userId);

        user.setEnabled(true);

        BankUser savedUser = bankUserRepository.saveAndFlush(user);

        return identityMapper.toIdentityResponse(savedUser);
    }

    @Transactional
    public IdentityResponse disableIdentity(UUID userId) {
        BankUser user = getIdentityForUpdate(userId);

        user.setEnabled(false);

        BankUser savedUser = bankUserRepository.saveAndFlush(user);

        return identityMapper.toIdentityResponse(savedUser);
    }

    @Transactional
    public IdentityResponse lockIdentity(
            UUID userId,
            Instant lockedUntil
    ) {
        if (lockedUntil != null && !lockedUntil.isAfter(Instant.now())) {
            throw new InvalidIdentityStateException(
                    "Lock expiry must be a future timestamp"
            );
        }

        BankUser user = getIdentityForUpdate(userId);

        user.setAccountNonLocked(false);
        user.setLockedUntil(lockedUntil);

        BankUser savedUser = bankUserRepository.saveAndFlush(user);

        return identityMapper.toIdentityResponse(savedUser);
    }

    @Transactional
    public IdentityResponse unlockIdentity(UUID userId) {
        BankUser user = getIdentityForUpdate(userId);

        user.setAccountNonLocked(true);
        user.setLockedUntil(null);
        user.setFailedLoginAttempts(0);

        BankUser savedUser = bankUserRepository.saveAndFlush(user);

        return identityMapper.toIdentityResponse(savedUser);
    }

    private BankUser getIdentityForUpdate(UUID userId) {
        return bankUserRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new IdentityNotFoundException(userId));
    }
}