package com.gideonheritagebank.IDENTITY_SERVICE.service;

import com.gideonheritagebank.IDENTITY_SERVICE.dto.request.UpdateStaffProfileRequest;
import com.gideonheritagebank.IDENTITY_SERVICE.dto.response.StaffProfileResponse;
import com.gideonheritagebank.IDENTITY_SERVICE.entity.BankUser;
import com.gideonheritagebank.IDENTITY_SERVICE.entity.StaffProfile;
import com.gideonheritagebank.IDENTITY_SERVICE.enums.UserType;
import com.gideonheritagebank.IDENTITY_SERVICE.exception.BadRequest;
import com.gideonheritagebank.IDENTITY_SERVICE.exception.ConflictException;
import com.gideonheritagebank.IDENTITY_SERVICE.exception.IdentityNotFoundException;
import com.gideonheritagebank.IDENTITY_SERVICE.exception.ResourceNotFoundException;
import com.gideonheritagebank.IDENTITY_SERVICE.mapper.IdentityMapper;
import com.gideonheritagebank.IDENTITY_SERVICE.repository.BankUserRepository;
import com.gideonheritagebank.IDENTITY_SERVICE.repository.StaffProfileRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StaffProfileService {

    private final StaffProfileRepository staffProfileRepository;
    private final BankUserRepository bankUserRepository;
    private final IdentityMapper identityMapper;

    /* GET STAFF PROFILE BY STAFF PROFILE ID */
    @PreAuthorize("hasRole('ADMIN')")
    public StaffProfileResponse getStaffProfileById(
            UUID staffProfileId
    ) {
        StaffProfile profile = staffProfileRepository.findWithUserAndDepartmentById(staffProfileId)
                .orElseThrow(() -> new ResourceNotFoundException(
                                "Staff profile not found: " + staffProfileId
                        )
                );

        return identityMapper.toStaffProfileResponse(profile);
    }

    /* GET STAFF PROFILE BY BANK USER ID */
    @PreAuthorize("hasRole('ADMIN')")
    public StaffProfileResponse getStaffProfileByUserId(
            UUID userId
    ) {
        StaffProfile profile = staffProfileRepository
                .findWithUserAndDepartmentByUser_Id(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Staff profile not found for user: " + userId
                        )
                );

        return identityMapper.toStaffProfileResponse(profile);
    }

    /* UPDATE STAFF PERSONAL PROFILE */
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public StaffProfileResponse updateStaffProfile(
            UUID staffProfileId,
            UpdateStaffProfileRequest request
    ) {

        if (request == null) {
            throw new BadRequest(
                    "Staff profile update request is required"
            );
        }

        StaffProfile profile = staffProfileRepository.findByIdForUpdate(staffProfileId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Staff profile not found: " + staffProfileId
                        )
                );

        BankUser staffUser = profile.getUser();

        if (staffUser == null || staffUser.getUserType() != UserType.STAFF) {

            throw new BadRequest(
                    "The requested profile is not associated with a staff identity"
            );
        }

        /*
         * Lock the associated identity record before modifying
         * personal information.
         */
        staffUser = bankUserRepository.findByIdForUpdate(staffUser.getId())
                .orElseThrow(() ->
                        new IdentityNotFoundException(profile.getUser().getId())
                );

        if (staffUser.isIdentityClosed()) {
            throw new BadRequest("A closed staff identity cannot be updated");
        }

        updateFirstName(staffUser, request.firstName());
        updateLastName(staffUser, request.lastName());
        updatePhoneNumber(staffUser, request.phoneNumber());
        updateProfilePhoto(staffUser, request.profilePhotoUrl());

        bankUserRepository.saveAndFlush(staffUser);

        return identityMapper.toStaffProfileResponse(profile);
    }

    private void updateFirstName(
            BankUser user,
            String firstName
    ) {
        if (firstName == null) {
            return;
        }

        String normalized = normalizeName(firstName);

        if (normalized.isBlank()) {
            throw new BadRequest("First name cannot be blank");
        }

        user.setFirstName(normalized);
    }

    private void updateLastName(
            BankUser user,
            String lastName
    ) {
        if (lastName == null) {
            return;
        }

        String normalized = normalizeName(lastName);

        if (normalized.isBlank()) {
            throw new BadRequest("Last name cannot be blank");
        }

        user.setLastName(normalized);
    }

    private void updatePhoneNumber(
            BankUser user,
            String phoneNumber
    ) {
        if (phoneNumber == null) {
            return;
        }

        String normalized = phoneNumber.trim();

        if (normalized.isBlank()) {
            throw new BadRequest("Phone number cannot be blank");
        }

        if (!normalized.equals(user.getPhoneNumber())
                && bankUserRepository.existsByPhoneNumberAndIdNot(
                normalized,
                user.getId()
        )) {
            throw new ConflictException("The phone number is already registered");
        }

        user.setPhoneNumber(normalized);
    }

    private void updateProfilePhoto(
            BankUser user,
            String profilePhotoUrl
    ) {
        if (profilePhotoUrl == null) {
            return;
        }

        String normalized = profilePhotoUrl.trim();

        user.setProfilePhotoUrl(normalized.isBlank() ? null : normalized);
    }

    private String normalizeName(String value) {

        return value == null ? "" : value.trim().replaceAll("\\s+", " ");
    }
}
