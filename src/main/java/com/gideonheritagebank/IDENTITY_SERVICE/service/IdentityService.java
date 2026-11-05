package com.gideonheritagebank.IDENTITY_SERVICE.service;

import com.gideonheritagebank.IDENTITY_SERVICE.component.CustomerNumberGenerator;
import com.gideonheritagebank.IDENTITY_SERVICE.dto.request.RegisterRequest;
import com.gideonheritagebank.IDENTITY_SERVICE.dto.response.IdentityResponse;
import com.gideonheritagebank.IDENTITY_SERVICE.entity.BankUser;
import com.gideonheritagebank.IDENTITY_SERVICE.entity.Role;
import com.gideonheritagebank.IDENTITY_SERVICE.enums.RoleName;
import com.gideonheritagebank.IDENTITY_SERVICE.enums.UserType;
import com.gideonheritagebank.IDENTITY_SERVICE.exception.EmailAlreadyRegisteredException;
import com.gideonheritagebank.IDENTITY_SERVICE.exception.IdentityNotFoundException;
import com.gideonheritagebank.IDENTITY_SERVICE.exception.InvalidIdentityStateException;
import com.gideonheritagebank.IDENTITY_SERVICE.exception.InvalidRegistrationException;
import com.gideonheritagebank.IDENTITY_SERVICE.exception.PhoneNumberAlreadyRegisteredException;
import com.gideonheritagebank.IDENTITY_SERVICE.exception.RoleConfigurationException;
import com.gideonheritagebank.IDENTITY_SERVICE.mapper.IdentityMapper;
import com.gideonheritagebank.IDENTITY_SERVICE.repository.BankUserRepository;
import com.gideonheritagebank.IDENTITY_SERVICE.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class IdentityService {

    private final BankUserRepository bankUserRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final IdentityMapper identityMapper;
    private final EmailVerificationService emailVerificationService;
    private final CustomerNumberGenerator customerNumberGenerator;
    private final AuthenticationService authenticationService;

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
                passwordHash,
                customerNumberGenerator.generate()
        );

        user.addRole(customerRole);

        BankUser savedUser;

        try {
            /*
             * Flush here so database uniqueness conflicts surface within
             * this method and can be translated correctly.
             */
            savedUser = bankUserRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException exception) {
            throw translateRegistrationConflict(exception);
        }

        /*
         * Creates the hashed verification token and registers an
         * after-commit event. Both participate in this transaction.
         */
        emailVerificationService.createVerificationToken(savedUser);

        return identityMapper.toIdentityResponse(savedUser);
    }

    private void validatePasswordConfirmation(RegisterRequest request) {

        if (request.getPassword() == null || request.getConfirmPassword() == null
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
    public IdentityResponse enableCustomer(
            UUID customerId,
            String reason
    ) {
        normalizeReason(reason);

        BankUser customer = getCustomerForUpdate(customerId);

        if (customer.isEnabled()) {
            throw new InvalidIdentityStateException(
                    "Customer identity is already enabled."
            );
        }

        customer.setEnabled(true);
        BankUser savedCustomer = bankUserRepository.saveAndFlush(customer);
        return identityMapper.toIdentityResponse(savedCustomer);
    }

    @Transactional
    public IdentityResponse disableCustomer(
            UUID customerId,
            String reason
    ) {
        normalizeReason(reason);

        BankUser customer = getCustomerForUpdate(customerId);

        if (!customer.isEnabled()) {
            throw new InvalidIdentityStateException(
                    "Customer identity is already disabled."
            );
        }

        customer.setEnabled(false);
        authenticationService.revokeAllActiveRefreshTokens(customer);
        BankUser savedCustomer = bankUserRepository.saveAndFlush(customer);

        return identityMapper.toIdentityResponse(savedCustomer);
    }

    @Transactional
    public IdentityResponse lockCustomer(
            UUID customerId,
            Instant lockedUntil,
            String reason
    ) {
        normalizeReason(reason);

        if (lockedUntil != null && !lockedUntil.isAfter(Instant.now())) {
            throw new InvalidIdentityStateException(
                    "Lock expiry must be a future timestamp."
            );
        }

        BankUser customer = getCustomerForUpdate(customerId);

        if (!customer.isAccountNonLocked()) {
            throw new InvalidIdentityStateException(
                    "Customer identity is already locked."
            );
        }

        customer.setAccountNonLocked(false);
        customer.setLockedUntil(lockedUntil);
        authenticationService.revokeAllActiveRefreshTokens(customer);
        BankUser savedCustomer = bankUserRepository.saveAndFlush(customer);

        return identityMapper.toIdentityResponse(savedCustomer);
    }

    @Transactional
    public IdentityResponse unlockCustomer(
            UUID customerId,
            String reason
    ) {
        normalizeReason(reason);
        BankUser customer = getCustomerForUpdate(customerId);

        if (customer.isAccountNonLocked()) {
            throw new InvalidIdentityStateException(
                    "Customer identity is not locked."
            );
        }

        customer.setAccountNonLocked(true);
        customer.setLockedUntil(null);
        customer.setFailedLoginAttempts(0);
        BankUser savedCustomer = bankUserRepository.saveAndFlush(customer);

        return identityMapper.toIdentityResponse(savedCustomer);
    }

    @Transactional
    public IdentityResponse suspendCustomer(
            UUID customerId,
            String reason
    ) {
        String normalizedReason = normalizeReason(reason);
        BankUser customer = getCustomerForUpdate(customerId);

        if (customer.isSuspended()) {
            throw new InvalidIdentityStateException(
                    "Customer identity is already suspended."
            );
        }

        customer.suspend(normalizedReason);
        authenticationService.revokeAllActiveRefreshTokens(customer);
        BankUser savedCustomer = bankUserRepository.saveAndFlush(customer);

        return identityMapper.toIdentityResponse(savedCustomer);
    }

    @Transactional
    public IdentityResponse unsuspendCustomer(
            UUID customerId,
            String reason
    ) {
        normalizeReason(reason);
        BankUser customer = getCustomerForUpdate(customerId);

        if (!customer.isSuspended()) {
            throw new InvalidIdentityStateException(
                    "Customer identity is not suspended."
            );
        }

        customer.unsuspend();
        BankUser savedCustomer = bankUserRepository.saveAndFlush(customer);

        return identityMapper.toIdentityResponse(savedCustomer);
    }

    private BankUser getCustomerForUpdate(UUID customerId) {

        BankUser customer = bankUserRepository.findByIdForUpdate(customerId)
                .orElseThrow(() -> new IdentityNotFoundException(customerId));

        if (customer.getUserType() != UserType.CUSTOMER) {
            throw new IdentityNotFoundException(customerId);
        }

        return customer;
    }

    private String normalizeReason(String reason) {

        if (reason == null || reason.isBlank()) {
            throw new InvalidIdentityStateException(
                    "A reason must be provided for this identity operation."
            );
        }

        String normalized = reason.trim();

        if (normalized.length() < 5 || normalized.length() > 1000) {
            throw new InvalidIdentityStateException(
                    "Reason must contain between 5 and 1000 characters."
            );
        }

        return normalized;
    }
}