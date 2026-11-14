package com.gideonheritagebank.IDENTITY_SERVICE.service;

import com.gideonheritagebank.IDENTITY_SERVICE.dto.request.*;
import com.gideonheritagebank.IDENTITY_SERVICE.dto.response.StaffClosureHistoryResponse;
import com.gideonheritagebank.IDENTITY_SERVICE.dto.response.StaffLifecycleResponse;
import com.gideonheritagebank.IDENTITY_SERVICE.entity.BankUser;
import com.gideonheritagebank.IDENTITY_SERVICE.entity.RefreshToken;
import com.gideonheritagebank.IDENTITY_SERVICE.entity.StaffClosureHistory;
import com.gideonheritagebank.IDENTITY_SERVICE.entity.StaffProfile;
import com.gideonheritagebank.IDENTITY_SERVICE.enums.EmploymentStatus;
import com.gideonheritagebank.IDENTITY_SERVICE.enums.UserType;
import com.gideonheritagebank.IDENTITY_SERVICE.exception.*;
import com.gideonheritagebank.IDENTITY_SERVICE.mapper.IdentityMapper;
import com.gideonheritagebank.IDENTITY_SERVICE.repository.BankUserRepository;
import com.gideonheritagebank.IDENTITY_SERVICE.repository.RefreshTokenRepository;
import com.gideonheritagebank.IDENTITY_SERVICE.repository.StaffClosureHistoryRepository;
import com.gideonheritagebank.IDENTITY_SERVICE.repository.StaffProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StaffLifecycleService {

    private final BankUserRepository bankUserRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final StaffEmploymentService staffEmploymentService;
    private final StaffClosureHistoryRepository staffClosureHistoryRepository;
    private final StaffProfileRepository staffProfileRepository;
    private final IdentityMapper identityMapper;

    /* GET STAFF LIFECYCLE STATUS */

    @PreAuthorize("hasRole('ADMIN')")
    public StaffLifecycleResponse getStaffLifecycleStatus(UUID staffUserId) {

        BankUser staff = findStaff(staffUserId);

        return identityMapper.toStaffLifecycleResponse(staff);
    }

    /* ENABLE STAFF IDENTITY */
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public StaffLifecycleResponse enableStaff(UUID staffUserId) {

        BankUser staff = getStaffForUpdate(staffUserId);
        ensureIdentityNotClosed(staff);
        ensureStaffNotTerminated(staff);
        ensureNotSuspended(staff);
        ensureNotLocked(staff);

        if (!staff.isEmailVerified()) {
            throw new BadRequest(
                    "Staff identity cannot be enabled before email verification"
            );
        }

        if (!staff.isEnabled()) {
            staff.setEnabled(true);

            bankUserRepository.saveAndFlush(staff);
        }

        return identityMapper.toStaffLifecycleResponse(staff);
    }

    /* DISABLE STAFF IDENTITY */
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public StaffLifecycleResponse disableStaff(UUID staffUserId) {

        BankUser staff = getStaffForUpdate(staffUserId);
        ensureIdentityNotClosed(staff);

        if (staff.isEnabled()) {

            staff.setEnabled(false);
            revokeAllRefreshTokens(staff.getId());
            bankUserRepository.saveAndFlush(staff);
        }

        return identityMapper.toStaffLifecycleResponse(staff);
    }

    /* SUSPEND STAFF IDENTITY */
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public StaffLifecycleResponse suspendStaff(
            UUID staffUserId,
            SuspendStaffRequest request
    ) {
        if (request == null || request.reason() == null || request.reason().isBlank()) {

            throw new BadRequest("Suspension reason is required");
        }

        BankUser staff = getStaffForUpdate(staffUserId);
        ensureIdentityNotClosed(staff);
        ensureStaffNotTerminated(staff);

        String reason = request.reason().trim();

        /* Suspension prevents authentication. */
        staff.suspend(reason);
        staff.setEnabled(false);

        /* Revoke existing refresh sessions. */
        revokeAllRefreshTokens(staff.getId());
        bankUserRepository.saveAndFlush(staff);

        return identityMapper.toStaffLifecycleResponse(staff);
    }

    /* UNSUSPEND STAFF IDENTITY */
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public StaffLifecycleResponse unsuspendStaff(UUID staffUserId) {

        BankUser staff = getStaffForUpdate(staffUserId);
        ensureIdentityNotClosed(staff);
        ensureStaffNotTerminated(staff);

        if (staff.isSuspended()) {

            staff.unsuspend();

            /*
             * Deliberately do not enable the identity here.
             * An administrator must explicitly call the enable endpoint
             * after all other restrictions have been resolved.
             */

            bankUserRepository.saveAndFlush(staff);
        }

        return identityMapper.toStaffLifecycleResponse(staff);
    }

    /* LOCK STAFF IDENTITY*/
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public StaffLifecycleResponse lockStaff(
            UUID staffUserId,
            LockStaffRequest request
    ) {
        if (request == null || request.lockedUntil() == null) {
            throw new BadRequest("Lock expiry date is required");
        }

        Instant now = Instant.now();

        if (!request.lockedUntil().isAfter(now)) {
            throw new BadRequest(
                    "Lock expiry must be a future date and time"
            );
        }

        BankUser staff = getStaffForUpdate(staffUserId);

        ensureIdentityNotClosed(staff);
        ensureStaffNotTerminated(staff);

        staff.setAccountNonLocked(false);
        staff.setLockedUntil(request.lockedUntil());

        /*
         * Resetting failed attempts avoids carrying old failed-login
         * counts into a new security lock period.
         */
        staff.setFailedLoginAttempts(0);
        revokeAllRefreshTokens(staff.getId());
        bankUserRepository.saveAndFlush(staff);

        return identityMapper.toStaffLifecycleResponse(staff);
    }

    /* UNLOCK STAFF IDENTITY */
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public StaffLifecycleResponse unlockStaff(UUID staffUserId) {

        BankUser staff = getStaffForUpdate(staffUserId);

        ensureIdentityNotClosed(staff);
        ensureStaffNotTerminated(staff);

        if (!staff.isAccountNonLocked() || staff.getLockedUntil() != null) {

            staff.setAccountNonLocked(true);
            staff.setLockedUntil(null);
            staff.setFailedLoginAttempts(0);

            /* Unlocking does not automatically enable the account. */
            bankUserRepository.saveAndFlush(staff);
        }

        return identityMapper.toStaffLifecycleResponse(staff);
    }

    /*
     * DEACTIVATE STAFF IDENTITY
     * Deactivation terminates the staff member's employment.
     * It is not the same as temporarily disabling an identity.
     * Employment history is maintained by StaffEmploymentService.
     */
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public StaffLifecycleResponse deactivateStaff(
            UUID staffUserId,
            DeactivateStaffRequest request,
            UUID changedByUserId
    ) {
        if (request == null) {
            throw new BadRequest("Deactivation request is required");
        }

        if (changedByUserId == null) {
            throw new BadRequest(
                    "The authenticated administrator identity is required"
            );
        }

        BankUser staff = getStaffForUpdate(staffUserId);

        ensureIdentityNotClosed(staff);

        StaffProfile profile = getStaffProfile(staff);

        if (profile.getEmploymentStatus() == EmploymentStatus.TERMINATED) {
            throw new BadRequest(
                    "This staff member's employment has already been terminated"
            );
        }

        LocalDate effectiveDate = request.effectiveDate();

        if (effectiveDate == null) {
            throw new BadRequest("Deactivation effective date is required");
        }

        if (effectiveDate.isAfter(LocalDate.now())) {
            throw new BadRequest(
                    "Deactivation effective date cannot be in the future"
            );
        }

        if (effectiveDate.isBefore(profile.getAssignmentStartDate())) {
            throw new BadRequest(
                    "Deactivation date cannot be before the current assignment start date"
            );
        }

        /*
         * Build a termination request using the existing employment
         * assignment values. This preserves the current department,
         * branch, job title, employment type and reporting manager
         * in the final employment-history record.
         */
        UpdateStaffEmploymentRequest employmentRequest = new UpdateStaffEmploymentRequest(
                profile.getDepartment() == null
                        ? null : profile.getDepartment().getId(),

                profile.getBranchId(),
                profile.getJobTitle(),
                profile.getEmploymentType(),
                EmploymentStatus.TERMINATED,
                effectiveDate,
                effectiveDate,
                profile.getReportingManagerId(),
                request.reason().trim()
        );


        /*
         * Reuse the existing employment service.
         *
         * It:
         * - Locks the staff profile.
         * - Closes the previous history record.
         * - Updates the current employment status.
         * - Creates the termination history record.
         * - Records changedByUserId.
         */
        staffEmploymentService.updateEmployment(
                profile.getId(),
                employmentRequest,
                changedByUserId
        );

        /* Disable authentication after terminating employment. */
        staff.setEnabled(false);
        revokeAllRefreshTokens(staff.getId());
        bankUserRepository.saveAndFlush(staff);

        return identityMapper.toStaffLifecycleResponse(staff);
    }

    /*
     * CLOSE STAFF IDENTITY
     * Closure is a permanent identity-level operation.
     * The staff member must already have terminated employment.
     */


    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public StaffLifecycleResponse closeStaff(
            UUID staffUserId,
            CloseStaffRequest request,
            UUID adminUserId
    ) {

        if (request == null) {
            throw new BadRequest("Staff closure request is required.");
        }

        if (adminUserId == null) {
            throw new BusinessException(
                    "The administrator performing this closure could not be identified."
            );
        }

        String reason = request.reason().trim();

        if (reason.isBlank()) {
            throw new BadRequest("A closure reason is required.");
        }

        if (reason.length() > 1000) {
            throw new BadRequest(
                    "Closure reason cannot exceed 1000 characters."
            );
        }

        Instant retentionReviewAt = request.retentionReviewAt();

        if (retentionReviewAt == null
                || !retentionReviewAt.isAfter(Instant.now())) {
            throw new BadRequest(
                    "Retention review date must be in the future."
            );
        }

        // 1. Lock the identity first.
        BankUser user = bankUserRepository.findByIdForUpdate(staffUserId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Staff identity not found."
                ));

        if (user.getUserType() != UserType.STAFF) {
            throw new ConflictException(
                    "Only staff identities can be closed through this operation."
            );
        }

        if (user.isIdentityClosed()) {
            throw new ConflictException(
                    "This staff identity has already been closed."
            );
        }

        // 2. Lock the staff profile.
        StaffProfile profile = staffProfileRepository
                .findByUserIdForUpdate(staffUserId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Staff profile not found."
                ));

        if (profile.getEmploymentStatus() != EmploymentStatus.TERMINATED) {
            throw new ConflictException(
                    "Staff employment must be terminated before identity closure."
            );
        }

        // 3. Prevent duplicate closure-history records.
        if (staffClosureHistoryRepository.existsByUserId(staffUserId)) {
            throw new ConflictException(
                    "A closure history record already exists for this staff identity."
            );
        }

        Instant closedAt = Instant.now();

        // 4. Capture the final staff details for historical purposes.
        StaffClosureHistory closureHistory = new StaffClosureHistory(
                profile,
                user.getId(),
                profile.getEmployeeNumber(),
                profile.getStaffRole(),
                reason,
                adminUserId,
                closedAt,
                retentionReviewAt
        );

        staffClosureHistoryRepository.save(closureHistory);

        // 5. Close the identity.
        user.closeIdentity(reason, retentionReviewAt);

        // 6. Revoke outstanding refresh tokens.
        revokeAllRefreshTokens(staffUserId);

        // 7. Persist the updated identity.
        BankUser savedUser = bankUserRepository.saveAndFlush(user);

        // 8. Return the mapped response.
        return identityMapper.toStaffLifecycleResponse(savedUser);
    }


    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN')")
    public Page<StaffClosureHistoryResponse> getStaffClosureHistory(
            Pageable pageable
    ) {
        Page<StaffClosureHistory> historyPage =
                staffClosureHistoryRepository.findAllByOrderByClosedAtDesc(pageable);

        return historyPage.map(
                identityMapper::toStaffClosureHistoryResponse
        );
    }


    /* FIND STAFF IDENTITY */
    private BankUser findStaff(UUID staffUserId) {

        if (staffUserId == null) {
            throw new BadRequest("Staff user ID is required");
        }

        BankUser staff = bankUserRepository.findByIdAndUserType(
                        staffUserId,
                        UserType.STAFF
                )
                .orElseThrow(() ->
                        new IdentityNotFoundException(staffUserId)
                );

        getStaffProfile(staff);

        return staff;
    }

    /*
     * FIND STAFF IDENTITY FOR UPDATE
     * PESSIMISTIC_WRITE prevents concurrent lifecycle changes
     * against the same staff identity.
     */
    private BankUser getStaffForUpdate(UUID staffUserId) {

        if (staffUserId == null) {
            throw new BadRequest("Staff user ID is required");
        }

        BankUser staff = bankUserRepository.findByIdForUpdate(staffUserId)
                .orElseThrow(() -> new IdentityNotFoundException(staffUserId));

        if (staff.getUserType() != UserType.STAFF) {
            throw new BadRequest("The requested identity is not a staff identity");
        }

        getStaffProfile(staff);

        return staff;
    }

    /* GET STAFF PROFILE */
    private StaffProfile getStaffProfile(BankUser staff) {

        if (staff.getStaffProfile() == null) {
            throw new ResourceNotFoundException(
                    "Staff profile not found for identity: " + staff.getId()
            );
        }

        return staff.getStaffProfile();
    }

    /* LIFECYCLE VALIDATION HELPERS */
    private void ensureIdentityNotClosed(BankUser staff) {

        if (staff.isIdentityClosed()) {
            throw new BadRequest("This staff identity has already been permanently closed");
        }
    }

    private void ensureStaffNotTerminated(BankUser staff) {

        StaffProfile profile = getStaffProfile(staff);

        if (profile.getEmploymentStatus() == EmploymentStatus.TERMINATED) {
            throw new BadRequest("This staff member's employment has been terminated");
        }
    }

    private void ensureNotSuspended(BankUser staff) {

        if (staff.isSuspended()) {
            throw new BadRequest("Suspended staff identities must be unsuspended first");
        }
    }

    private void ensureNotLocked(BankUser staff) {

        if (!staff.isAccountNonLocked()) {
            throw new BadRequest("Locked staff identities must be unlocked first");
        }
    }

    /* REVOKE ALL REFRESH TOKENS */
    private void revokeAllRefreshTokens(UUID userId) {

        List<RefreshToken> refreshTokens =
                refreshTokenRepository.findAllByUser_Id(userId);

        Instant now = Instant.now();
        boolean changed = false;

        for (RefreshToken token : refreshTokens) {

            if (token.getRevokedAt() == null) {

                token.setRevokedAt(now);
                changed = true;
            }
        }

        if (changed) {
            refreshTokenRepository.saveAllAndFlush(refreshTokens);
        }
    }
}