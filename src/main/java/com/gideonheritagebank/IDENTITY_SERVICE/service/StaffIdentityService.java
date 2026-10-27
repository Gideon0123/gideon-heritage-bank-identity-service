package com.gideonheritagebank.IDENTITY_SERVICE.service;

import com.gideonheritagebank.IDENTITY_SERVICE.dto.request.StaffIdentityCreationRequest;
import com.gideonheritagebank.IDENTITY_SERVICE.dto.response.IdentityResponse;
import com.gideonheritagebank.IDENTITY_SERVICE.entity.BankUser;
import com.gideonheritagebank.IDENTITY_SERVICE.entity.Role;
import com.gideonheritagebank.IDENTITY_SERVICE.entity.StaffProfile;
import com.gideonheritagebank.IDENTITY_SERVICE.enums.RoleName;
import com.gideonheritagebank.IDENTITY_SERVICE.enums.StaffRole;
import com.gideonheritagebank.IDENTITY_SERVICE.exception.*;
import com.gideonheritagebank.IDENTITY_SERVICE.mapper.IdentityMapper;
import com.gideonheritagebank.IDENTITY_SERVICE.repository.BankUserRepository;
import com.gideonheritagebank.IDENTITY_SERVICE.repository.RoleRepository;
import com.gideonheritagebank.IDENTITY_SERVICE.repository.StaffProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.Base64;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class StaffIdentityService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final BankUserRepository bankUserRepository;
    private final RoleRepository roleRepository;
    private final StaffProfileRepository staffProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final IdentityMapper identityMapper;

    @Transactional
    public IdentityResponse createStaffIdentity(
            StaffIdentityCreationRequest request
    ) {
        String firstName = normalizeName(request.getFirstName());
        String lastName = normalizeName(request.getLastName());
        String email = normalizeEmail(request.getEmail());
        String phoneNumber = normalizePhoneNumber(request.getPhoneNumber());
        String employeeNumber = normalizeEmployeeNumber(
                request.getEmployeeNumber()
        );

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

        RoleName specificRoleName = RoleName.valueOf(
                request.getStaffRole().name()
        );

        Role specificStaffRole = findRequiredRole(specificRoleName);

        /*
         * Generate a random, undisclosed temporary secret.
         *
         * The raw secret is never returned, logged, or persisted.
         * The account remains disabled until the proper onboarding
         * and credential-setup flow is implemented.
         */
        String temporarySecret = generateTemporarySecret();
        String passwordHash = passwordEncoder.encode(temporarySecret);

        // Do not retain the raw secret beyond the hashing operation.
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
            /*
             * Save the user first to obtain its generated UUID.
             * The surrounding transaction keeps the user and profile
             * creation atomic.
             */
            BankUser savedUser = bankUserRepository.saveAndFlush(staffUser);

            StaffProfile profile = StaffProfile.create(
                    savedUser,
                    employeeNumber,
                    request.getStaffRole(),
                    request.getHireDate()
            );

            savedUser.assignStaffProfile(profile);

            /*
             * Persist the profile explicitly. The one-to-one relationship
             * is owned by StaffProfile.user.
             */
            staffProfileRepository.saveAndFlush(profile);

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
}