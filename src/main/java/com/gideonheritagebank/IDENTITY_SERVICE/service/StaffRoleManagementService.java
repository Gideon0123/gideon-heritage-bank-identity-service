package com.gideonheritagebank.IDENTITY_SERVICE.service;

import com.gideonheritagebank.IDENTITY_SERVICE.dto.request.AssignStaffRoleRequest;
import com.gideonheritagebank.IDENTITY_SERVICE.dto.request.ChangeStaffRoleRequest;
import com.gideonheritagebank.IDENTITY_SERVICE.dto.request.RemoveStaffRoleRequest;
import com.gideonheritagebank.IDENTITY_SERVICE.dto.response.StaffRoleHistoryResponse;
import com.gideonheritagebank.IDENTITY_SERVICE.dto.response.StaffRoleResponse;
import com.gideonheritagebank.IDENTITY_SERVICE.entity.BankUser;
import com.gideonheritagebank.IDENTITY_SERVICE.entity.RefreshToken;
import com.gideonheritagebank.IDENTITY_SERVICE.entity.Role;
import com.gideonheritagebank.IDENTITY_SERVICE.entity.StaffProfile;
import com.gideonheritagebank.IDENTITY_SERVICE.entity.StaffRoleHistory;
import com.gideonheritagebank.IDENTITY_SERVICE.enums.EmploymentStatus;
import com.gideonheritagebank.IDENTITY_SERVICE.enums.RoleName;
import com.gideonheritagebank.IDENTITY_SERVICE.enums.StaffRole;
import com.gideonheritagebank.IDENTITY_SERVICE.enums.StaffRoleChangeType;
import com.gideonheritagebank.IDENTITY_SERVICE.enums.UserType;
import com.gideonheritagebank.IDENTITY_SERVICE.exception.BadRequest;
import com.gideonheritagebank.IDENTITY_SERVICE.exception.ConflictException;
import com.gideonheritagebank.IDENTITY_SERVICE.exception.ResourceNotFoundException;
import com.gideonheritagebank.IDENTITY_SERVICE.mapper.IdentityMapper;
import com.gideonheritagebank.IDENTITY_SERVICE.repository.BankUserRepository;
import com.gideonheritagebank.IDENTITY_SERVICE.repository.RefreshTokenRepository;
import com.gideonheritagebank.IDENTITY_SERVICE.repository.RoleRepository;
import com.gideonheritagebank.IDENTITY_SERVICE.repository.StaffProfileRepository;
import com.gideonheritagebank.IDENTITY_SERVICE.repository.StaffRoleHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StaffRoleManagementService {

    private final BankUserRepository bankUserRepository;
    private final StaffProfileRepository staffProfileRepository;
    private final RoleRepository roleRepository;
    private final StaffRoleHistoryRepository staffRoleHistoryRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final IdentityMapper identityMapper;


    @Transactional(readOnly = true)
    public StaffRoleResponse getStaffRole(UUID staffUserId) {

        StaffProfile staffProfile = findStaffProfile(staffUserId);
        return identityMapper.toStaffRoleResponse(staffProfile);
    }

    @Transactional(readOnly = true)
    public Page<StaffRoleHistoryResponse> getStaffRoleHistory(
            UUID staffUserId,
            Pageable pageable
    ) {
        StaffProfile staffProfile = findStaffProfile(staffUserId);

        return staffRoleHistoryRepository.findAllByStaffProfile_IdOrderByChangedAtDesc(
                        staffProfile.getId(),
                        pageable
                ).map(identityMapper::toStaffRoleHistoryResponse);
    }

    @Transactional
    public StaffRoleResponse assignRole(
            UUID staffUserId,
            AssignStaffRoleRequest request,
            UUID changedByUserId
    ) {
        StaffContext context = findStaffForUpdate(staffUserId);

        validateStaffCanReceiveRole(context);

        StaffProfile staffProfile = context.staffProfile();
        BankUser user = context.user();

        if (staffProfile.getStaffRole() != null) {
            throw new ConflictException(
                    "This staff member already has an operational role. Use the change-role operation instead."
            );
        }

        StaffRole requestedRole = validateOperationalRole(request.staffRole());
        Role operationalRole = findRole(requestedRole);
        Role genericStaffRole = findGenericStaffRole();

        user.addRole(genericStaffRole);
        user.addRole(operationalRole);

        staffProfile.setStaffRole(requestedRole);

        staffProfileRepository.save(staffProfile);
        bankUserRepository.save(user);

        recordRoleChange(
                staffProfile,
                null,
                requestedRole,
                StaffRoleChangeType.ASSIGNED,
                request.reason(),
                changedByUserId
        );

        revokeRefreshTokens(user.getId());

        return identityMapper.toStaffRoleResponse(staffProfile);
    }

    @Transactional
    public StaffRoleResponse changeRole(
            UUID staffUserId,
            ChangeStaffRoleRequest request,
            UUID changedByUserId
    ) {
        StaffContext context = findStaffForUpdate(staffUserId);

        validateStaffCanReceiveRole(context);
        StaffProfile staffProfile = context.staffProfile();
        BankUser user = context.user();

        StaffRole currentRole = staffProfile.getStaffRole();

        if (currentRole == null) {
            throw new ConflictException(
                    "This staff member does not currently have an operational role. Use the assign-role operation instead."
            );
        }

        StaffRole newRole = validateOperationalRole(request.newStaffRole());

        if (currentRole == newRole) {
            throw new BadRequest(
                    "The staff member already has this operational role."
            );
        }

        Role oldRoleEntity = findRole(currentRole);
        Role newRoleEntity = findRole(newRole);

        user.removeRole(oldRoleEntity);
        user.addRole(findGenericStaffRole());
        user.addRole(newRoleEntity);

        staffProfile.setStaffRole(newRole);

        staffProfileRepository.save(staffProfile);
        bankUserRepository.save(user);

        recordRoleChange(
                staffProfile,
                currentRole,
                newRole,
                StaffRoleChangeType.CHANGED,
                request.reason(),
                changedByUserId
        );

        revokeRefreshTokens(user.getId());

        return identityMapper.toStaffRoleResponse(staffProfile);
    }

    @Transactional
    public StaffRoleResponse removeRole(
            UUID staffUserId,
            RemoveStaffRoleRequest request,
            UUID changedByUserId
    ) {
        StaffContext context = findStaffForUpdate(staffUserId);

        validateStaffCanReceiveRole(context);

        StaffProfile staffProfile = context.staffProfile();
        BankUser user = context.user();

        StaffRole currentRole = staffProfile.getStaffRole();

        if (currentRole == null) {
            throw new ConflictException(
                    "This staff member does not currently have an operational role."
            );
        }

        Role currentRoleEntity = findRole(currentRole);

        user.removeRole(currentRoleEntity);

        // Never remove the generic STAFF identity role.
        user.addRole(findGenericStaffRole());

        staffProfile.setStaffRole(null);

        staffProfileRepository.save(staffProfile);
        bankUserRepository.save(user);

        recordRoleChange(
                staffProfile,
                currentRole,
                null,
                StaffRoleChangeType.REMOVED,
                request.reason(),
                changedByUserId
        );

        revokeRefreshTokens(user.getId());

        return identityMapper.toStaffRoleResponse(staffProfile);
    }

    private StaffContext findStaffForUpdate(UUID staffUserId) {

        BankUser user = bankUserRepository.findByIdForUpdate(staffUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff identity not found."));

        if (user.getUserType() != UserType.STAFF) {
            throw new BadRequest("The specified identity is not a staff identity.");
        }

        StaffProfile staffProfile = staffProfileRepository.findByUserIdForUpdate(staffUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff profile not found."));

        return new StaffContext(user, staffProfile);
    }

    private StaffProfile findStaffProfile(UUID staffUserId) {

        BankUser user = bankUserRepository.findById(staffUserId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Staff identity not found."
                ));

        if (user.getUserType() != UserType.STAFF) {
            throw new BadRequest(
                    "The specified identity is not a staff identity."
            );
        }

        return staffProfileRepository
                .findWithUserAndDepartmentByUser_Id(staffUserId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Staff profile not found."
                ));
    }

    private void validateStaffCanReceiveRole(StaffContext context) {

        BankUser user = context.user();
        StaffProfile staffProfile = context.staffProfile();

        if (user.isIdentityClosed()) {
            throw new ConflictException(
                    "Roles cannot be modified for a closed staff identity."
            );
        }

        if (user.isSuspended()) {
            throw new ConflictException(
                    "Roles cannot be modified for a suspended staff identity."
            );
        }

        if (staffProfile.getEmploymentStatus() == EmploymentStatus.TERMINATED) {
            throw new ConflictException(
                    "Roles cannot be modified for terminated staff."
            );
        }
    }

    private StaffRole validateOperationalRole(StaffRole staffRole) {

        if (staffRole == null) {
            throw new BadRequest("Staff role is required.");
        }

        if (staffRole == StaffRole.ADMIN) {
            throw new BadRequest(
                    "ADMIN cannot be assigned through ordinary staff role management."
            );
        }

        return staffRole;
    }

    private Role findRole(StaffRole staffRole) {

        RoleName roleName;

        try {
            roleName = RoleName.valueOf(staffRole.name());
        } catch (IllegalArgumentException exception) {
            throw new BadRequest(
                    "No matching identity role exists for " + staffRole.name() + "."
            );
        }

        return roleRepository.findByName(roleName)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "The identity role " + roleName + " has not been configured."
                ));
    }

    private Role findGenericStaffRole() {

        return roleRepository.findByName(RoleName.STAFF)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "The generic STAFF role has not been configured."
                ));
    }

    private void recordRoleChange(
            StaffProfile staffProfile,
            StaffRole previousRole,
            StaffRole newRole,
            StaffRoleChangeType changeType,
            String reason,
            UUID changedByUserId
    ) {
        StaffRoleHistory history = new StaffRoleHistory(
                staffProfile,
                previousRole,
                newRole,
                changeType,
                reason.trim(),
                changedByUserId
        );

        staffRoleHistoryRepository.save(history);
    }

    private void revokeRefreshTokens(UUID userId) {

        Instant revokedAt = Instant.now();

        for (RefreshToken refreshToken : refreshTokenRepository.findAllByUser_Id(userId)) {

            if (refreshToken.getRevokedAt() == null) {
                refreshToken.setRevokedAt(revokedAt);
            }
        }
    }


    private record StaffContext(
            BankUser user,
            StaffProfile staffProfile
    ) {
    }
}

