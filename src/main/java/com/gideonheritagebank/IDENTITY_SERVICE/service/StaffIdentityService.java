package com.gideonheritagebank.IDENTITY_SERVICE.service;

import com.gideonheritagebank.IDENTITY_SERVICE.dto.request.StaffIdentityCreationRequest;
import com.gideonheritagebank.IDENTITY_SERVICE.dto.response.IdentityResponse;
import com.gideonheritagebank.IDENTITY_SERVICE.entity.*;
import com.gideonheritagebank.IDENTITY_SERVICE.enums.EmploymentStatus;
import com.gideonheritagebank.IDENTITY_SERVICE.enums.RoleName;
import com.gideonheritagebank.IDENTITY_SERVICE.enums.StaffRole;
import com.gideonheritagebank.IDENTITY_SERVICE.exception.*;
import com.gideonheritagebank.IDENTITY_SERVICE.exception.IllegalStateException;
import com.gideonheritagebank.IDENTITY_SERVICE.mapper.IdentityMapper;
import com.gideonheritagebank.IDENTITY_SERVICE.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.gideonheritagebank.IDENTITY_SERVICE.event.StaffOnboardingRequestedEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;

@Service
@Slf4j
@RequiredArgsConstructor
public class StaffIdentityService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final BankUserRepository bankUserRepository;
    private final RoleRepository roleRepository;
    private final StaffProfileRepository staffProfileRepository;
    private final StaffEmploymentHistoryRepository staffEmploymentHistoryRepository;
    private final PasswordEncoder passwordEncoder;
    private final IdentityMapper identityMapper;
    private final StaffOnboardingTokenRepository staffOnboardingTokenRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${identity.staff-onboarding.token-ttl:PT30M}")
    private Duration onboardingTokenTtl;

    @Value("${identity.staff-onboarding.frontend-url:http://localhost:3000/staff/onboarding}")
    private String frontendOnboardingUrl;

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public IdentityResponse createStaffIdentity(
            StaffIdentityCreationRequest request
    ) {

        String firstName = normalizeName(request.getFirstName());
        String lastName = normalizeName(request.getLastName());
        String email = normalizeEmail(request.getEmail());
        String phoneNumber = normalizePhoneNumber(request.getPhoneNumber());
        String employeeNumber = normalizeEmployeeNumber(request.getEmployeeNumber());

        validateRequiredFields(
                firstName,
                lastName,
                email,
                phoneNumber,
                employeeNumber
        );

        validateHireDate(request.getHireDate());

        if (bankUserRepository.existsByEmail(email)) {
            throw new EmailAlreadyRegisteredException();
        }

        if (bankUserRepository.existsByPhoneNumber(phoneNumber)) {
            throw new PhoneNumberAlreadyRegisteredException();
        }

        if (staffProfileRepository.existsByEmployeeNumber(employeeNumber)) {
            throw new EmployeeNumberAlreadyRegisteredException();
        }

        Role staffRole = findRequiredRole(RoleName.STAFF);

        if (request.getStaffRole() == StaffRole.ADMIN) {
            throw new BadRequest("ADMIN cannot be assigned through ordinary staff provisioning.");
        }

        RoleName specificRoleName = mapStaffRoleToRoleName(
                request.getStaffRole()
        );

        Role specificStaffRole = findRequiredRole(specificRoleName);

        /*
         * Generate an undisclosed random initial secret.
         *
         * The employee will replace this with their own password
         * when completing onboarding.
         */
        String temporarySecret = generateTemporarySecret();
        String passwordHash = passwordEncoder.encode(temporarySecret);

        temporarySecret = null;

        BankUser staffUser = BankUser.createStaff(
                firstName,
                lastName,
                email,
                phoneNumber,
                passwordHash
        );

        staffUser.addRole(staffRole);
        staffUser.addRole(specificStaffRole);

        try {
            BankUser savedUser = bankUserRepository.saveAndFlush(staffUser);

            StaffProfile profile = StaffProfile.create(
                    savedUser,
                    employeeNumber,
                    request.getStaffRole(),
                    request.getHireDate()
            );

            /*
             * Explicitly initialize the employment fields.
             *
             * The employee has not completed onboarding yet, so the
             * initial employment status is PENDING.
             */
            profile.setAssignmentStartDate(request.getHireDate());
            profile.setEmploymentStatus(EmploymentStatus.PENDING);
            profile.setEmploymentEndDate(null);
            savedUser.assignStaffProfile(profile);

            staffProfileRepository.saveAndFlush(profile);

            /*
             * Create the initial employment-history record.
             * At provisioning time:
             * - No department has been assigned.
             * - No branch has been assigned.
             * - No job title has been assigned.
             * - No employment type has been specified.
             * - Employment status is PENDING.
             * I deliberately leave changedByUserId null here because
             * the provisioning service has not been given the authenticated
             * administrator's user ID.
             */
            StaffEmploymentHistory initialHistory = new StaffEmploymentHistory(
                    profile,
                    null,
                    null,
                    null,
                    null,
                    EmploymentStatus.PENDING,
                    request.getHireDate(),
                    "Initial employment record created during staff provisioning",
                    null
            );

            profile.addEmploymentHistory(initialHistory);

            staffEmploymentHistoryRepository.saveAndFlush(initialHistory);

            /* Generate a one-time onboarding token. */
            String rawToken = generateTemporarySecret();
            String tokenHash = hashOnboardingToken(rawToken);

            Instant now = Instant.now();
            Instant expiresAt = now.plus(onboardingTokenTtl);

            /* Invalidate previous outstanding onboarding tokens. */
            List<StaffOnboardingToken> previousTokens =
                    staffOnboardingTokenRepository.findAllByUser_Id(
                            savedUser.getId()
                    );

            for (StaffOnboardingToken previousToken : previousTokens) {

                if (!previousToken.isUsed() && !previousToken.isRevoked()) {

                    previousToken.revoke(now);
                }
            }

            if (!previousTokens.isEmpty()) {
                staffOnboardingTokenRepository.saveAllAndFlush(previousTokens);
            }

            StaffOnboardingToken onboardingToken = new StaffOnboardingToken(
                    savedUser,
                    tokenHash,
                    expiresAt
            );

            staffOnboardingTokenRepository.saveAndFlush(onboardingToken);

            /*
             * Construct the invitation URL.
             */
            String onboardingUrl = buildOnboardingUrl(rawToken);

            /*
             * The listener publishes to RabbitMQ only after commit.
             */
            eventPublisher.publishEvent(
                    new StaffOnboardingRequestedEvent(
                            savedUser.getId(),
                            savedUser.getEmail(),
                            savedUser.getFirstName(),
                            rawToken,
                            onboardingUrl,
                            expiresAt
                    )
            );

            log.info(
                    "Staff identity provisioned and onboarding initiated for userId={}",
                    savedUser.getId()
            );

            return identityMapper.toIdentityResponse(savedUser);

        } catch (DataIntegrityViolationException exception) {

            throw translateStaffConflict(exception);
        }
    }

    private Role findRequiredRole(RoleName roleName) {
        return roleRepository.findByName(roleName)
                .orElseThrow(() -> new RoleConfigurationException(
                        "Required role is missing from the database: "
                                + roleName
                ));
    }

    private void validateRequiredFields(
            String firstName,
            String lastName,
            String email,
            String phoneNumber,
            String employeeNumber
    ) {
        if (firstName.isBlank() || lastName.isBlank() || email.isBlank()
                || phoneNumber.isBlank() || employeeNumber.isBlank()) {

            throw new InvalidRegistrationException(
                    "Required staff identity fields must not be blank"
            );
        }
    }

    private void validateHireDate(LocalDate hireDate) {
        if (hireDate == null || hireDate.isAfter(LocalDate.now())) {
            throw new InvalidRegistrationException(
                    "Hire date must not be in the future"
            );
        }
    }

    private String normalizeName(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ");
    }

    private String normalizeEmail(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizePhoneNumber(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizeEmployeeNumber(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private String generateTemporarySecret() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);

        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private RuntimeException translateStaffConflict(
            DataIntegrityViolationException exception
    ) {
        String constraintName = findConstraintName(exception);

        if (constraintName == null) {
            return exception;
        }

        String normalizedConstraint = constraintName.toLowerCase(Locale.ROOT);

        if (normalizedConstraint.contains("email")) {
            return new EmailAlreadyRegisteredException();
        }

        if (normalizedConstraint.contains("phone_number")
                || normalizedConstraint.contains("phone")) {
            return new PhoneNumberAlreadyRegisteredException();
        }

        if (normalizedConstraint.contains("employee_number")) {
            return new EmployeeNumberAlreadyRegisteredException();
        }

        return exception;
    }

    private String findConstraintName(Throwable exception) {
        Throwable current = exception;

        while (current != null) {
            if (current instanceof
                    org.hibernate.exception.ConstraintViolationException violation) {
                return violation.getConstraintName();
            }

            current = current.getCause();
        }

        return null;
    }

    private RoleName mapStaffRoleToRoleName(StaffRole staffRole) {

        if (staffRole == null) {
            throw new InvalidRegistrationException(
                    "Staff position is required"
            );
        }

        return switch (staffRole) {
            case CASHIER -> RoleName.CASHIER;
            case MANAGER -> RoleName.MANAGER;
            case BRANCH_MANAGER -> RoleName.BRANCH_MANAGER;
            case ADMIN -> RoleName.ADMIN;
            case RISK_OFFICER -> RoleName.RISK_OFFICER;
            case SUPPORT_AGENT -> RoleName.SUPPORT_AGENT;
        };
    }

    private String hashOnboardingToken(String rawToken) {

        try {

            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(
                    rawToken.getBytes(StandardCharsets.UTF_8)
            );

            return HexFormat.of().formatHex(hash);

        } catch (NoSuchAlgorithmException exception) {

            throw new IllegalStateException("SHA-256 is not available");
        }
    }

    private String buildOnboardingUrl(String rawToken) {

        return UriComponentsBuilder
                .fromUriString(frontendOnboardingUrl)
                .queryParam("token", "{token}")
                .encode()
                .buildAndExpand(rawToken)
                .toUriString();
    }
}