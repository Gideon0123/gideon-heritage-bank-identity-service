package com.gideonheritagebank.IDENTITY_SERVICE.service;

import com.gideonheritagebank.IDENTITY_SERVICE.dto.request.ConvertCustomerToStaffRequest;
import com.gideonheritagebank.IDENTITY_SERVICE.dto.response.CustomerStaffConversionResponse;
import com.gideonheritagebank.IDENTITY_SERVICE.entity.*;
import com.gideonheritagebank.IDENTITY_SERVICE.enums.*;
import com.gideonheritagebank.IDENTITY_SERVICE.exception.BadRequest;
import com.gideonheritagebank.IDENTITY_SERVICE.exception.BusinessException;
import com.gideonheritagebank.IDENTITY_SERVICE.exception.ConflictException;
import com.gideonheritagebank.IDENTITY_SERVICE.exception.ResourceNotFoundException;
import com.gideonheritagebank.IDENTITY_SERVICE.mapper.IdentityMapper;
import com.gideonheritagebank.IDENTITY_SERVICE.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerStaffConversionService {

    private static final Set<StaffRole> ALLOWED_ROLES = EnumSet.of(
            StaffRole.CASHIER,
            StaffRole.MANAGER,
            StaffRole.BRANCH_MANAGER,
            StaffRole.RISK_OFFICER,
            StaffRole.SUPPORT_AGENT
    );

    private final BankUserRepository bankUserRepository;
    private final RoleRepository roleRepository;
    private final StaffProfileRepository staffProfileRepository;
    private final StaffEmploymentHistoryRepository staffEmploymentHistoryRepository;
    private final StaffRoleHistoryRepository staffRoleHistoryRepository;
    private final CustomerStaffConversionRepository customerStaffConversionRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final IdentityMapper identityMapper;

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public CustomerStaffConversionResponse convertCustomerToStaff(
            UUID customerUserId,
            ConvertCustomerToStaffRequest request,
            UUID convertedByUserId
    ) {
        validateRequest(request);

        if (convertedByUserId == null) {
            throw new BusinessException(
                    "The administrator performing this conversion could not be identified."
            );
        }

        // 1. Lock the customer identity to prevent concurrent modifications.
        BankUser user = bankUserRepository.findByIdForUpdate(customerUserId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Customer identity not found."
                ));

        // 2. Verify that this identity is currently a customer.
        if (user.getUserType() != UserType.CUSTOMER) {
            throw new ConflictException("Only customer identities can be converted to staff.");
        }

        // 3. Reject closed or suspended identities.
        if (user.isIdentityClosed()) {
            throw new ConflictException("A closed customer identity cannot be converted to staff.");
        }

        if (user.isSuspended()) {
            throw new ConflictException("A suspended customer identity cannot be converted to staff.");
        }

        // 4. Ensure the customer has a customer number.
        String customerNumber = user.getCustomerNumber();

        if (customerNumber == null || customerNumber.isBlank()) {
            throw new BusinessException(
                    "The customer identity does not have a valid customer number."
            );
        }

        // 5. Ensure the customer has not already been converted.
        if (customerStaffConversionRepository.existsByUser_Id(customerUserId)) {
            throw new ConflictException("A conversion record already exists for this identity.");
        }

        // 6. Ensure the customer does not already have a staff profile.
        if (staffProfileRepository.findByUser_Id(customerUserId).isPresent()) {
            throw new ConflictException("This identity already has a staff profile.");
        }

        // 7. Normalize and validate employee number.
        String employeeNumber = normalizeEmployeeNumber(request.getEmployeeNumber());

        if (staffProfileRepository.existsByEmployeeNumber(employeeNumber)) {
            throw new ConflictException(
                    "The employee number is already assigned to another staff member."
            );
        }

        // 8. Validate the requested operational role.
        StaffRole requestedStaffRole = request.getStaffRole();

        validateOperationalRole(requestedStaffRole);

        // 9. Retrieve the required roles.
        Role customerRole = findRole(RoleName.CUSTOMER);
        Role genericStaffRole = findRole(RoleName.STAFF);

        Role operationalRole = findRole(mapStaffRoleToRoleName(requestedStaffRole));

        // 10. Convert the identity from CUSTOMER to STAFF.
        user.removeRole(customerRole);
        user.addRole(genericStaffRole);
        user.addRole(operationalRole);

        user.setUserType(UserType.STAFF);

        /*
         * The converted identity must not automatically become an
         * enabled staff account.
         * An administrator will explicitly enable it through the
         * existing StaffLifecycleService after reviewing the conversion.
         */
        user.setEnabled(false);

        /*
         * Preserve:
         * - Existing password hash
         * - Existing email verification state
         * - Existing customer number
         * - Existing addresses
         * - Existing contact information
         */

        // 11. Create the staff profile.
        StaffProfile staffProfile = StaffProfile.create(
                user,
                employeeNumber,
                requestedStaffRole,
                request.getHireDate()
        );

        staffProfile.setAssignmentStartDate(request.getHireDate());
        staffProfile.setEmploymentStatus(EmploymentStatus.PENDING);
        staffProfile.setEmploymentEndDate(null);

        user.assignStaffProfile(staffProfile);

        staffProfileRepository.save(staffProfile);

        // 12. Create the initial employment-history record.
        StaffEmploymentHistory employmentHistory = new StaffEmploymentHistory(
                staffProfile,
                null,
                null,
                null,
                null,
                EmploymentStatus.PENDING,
                request.getHireDate(),
                request.getReason().trim(),
                convertedByUserId
        );

        staffEmploymentHistoryRepository.save(employmentHistory);

        // 13. Record the initial operational-role assignment.
        StaffRoleHistory roleHistory = new StaffRoleHistory(
                staffProfile,
                null,
                requestedStaffRole,
                StaffRoleChangeType.ASSIGNED,
                request.getReason().trim(),
                convertedByUserId
        );

        staffRoleHistoryRepository.save(roleHistory);

        // 14. Record the customer-to-staff conversion.
        CustomerStaffConversion conversion = new CustomerStaffConversion(
                user,
                customerNumber,
                employeeNumber,
                requestedStaffRole,
                request.getReason().trim(),
                convertedByUserId
        );

        CustomerStaffConversion savedConversion =
                customerStaffConversionRepository.save(conversion);

        // 15. Revoke all existing refresh tokens.
        revokeRefreshTokens(customerUserId);

        // 16. Persist the converted identity.
        bankUserRepository.saveAndFlush(user);

        log.info(
                "Customer identity {} converted to staff. Employee number: {}. Converted by: {}",
                customerUserId,
                employeeNumber,
                convertedByUserId
        );

        // 17. Return the mapped response.
        return identityMapper.toCustomerStaffConversionResponse(
                savedConversion
        );
    }

    private void validateRequest(ConvertCustomerToStaffRequest request) {

        if (request == null) {
            throw new BadRequest("Customer-to-staff conversion request is required.");
        }

        if (request.getStaffRole() == null) {
            throw new BadRequest("A staff role must be provided.");
        }

        if (request.getHireDate() == null) {
            throw new BadRequest("A hire date must be provided.");
        }

        if (request.getHireDate().isAfter(LocalDate.now())) {
            throw new BadRequest("Hire date cannot be in the future.");
        }

        if (request.getEmployeeNumber() == null || request.getEmployeeNumber().isBlank()) {
            throw new BadRequest("Employee number is required.");
        }

        if (request.getReason() == null || request.getReason().isBlank()) {
            throw new BadRequest("A reason for converting the customer is required.");
        }
    }

    private String normalizeEmployeeNumber(String employeeNumber) {

        String normalized = employeeNumber.trim().toUpperCase(Locale.ROOT);

        if (normalized.length() > 50) {
            throw new BadRequest("Employee number cannot exceed 50 characters.");
        }

        return normalized;
    }

    private void validateOperationalRole(StaffRole staffRole) {

        if (staffRole == null || !ALLOWED_ROLES.contains(staffRole)) {
            throw new BadRequest("The selected role is not an assignable operational staff role.");
        }
    }

    private Role findRole(RoleName roleName) {

        return roleRepository.findByName(roleName)
                .orElseThrow(() -> new BusinessException(
                        "Required role configuration is missing: " + roleName
                ));
    }

    private RoleName mapStaffRoleToRoleName(StaffRole staffRole) {

        try {
            return RoleName.valueOf(staffRole.name());
        } catch (IllegalArgumentException exception) {
            throw new BadRequest(
                    "No matching identity role exists for the selected staff role."
            );
        }
    }

    private void revokeRefreshTokens(UUID userId) {

        Instant revokedAt = Instant.now();

        refreshTokenRepository.findAllByUser_Id(userId)
                .stream()
                .filter(token -> token.getRevokedAt() == null)
                .forEach(token -> token.setRevokedAt(revokedAt));
    }
}
