package com.gideonheritagebank.IDENTITY_SERVICE.service;

import com.gideonheritagebank.IDENTITY_SERVICE.dto.request.CreateStaffAddressRequest;
import com.gideonheritagebank.IDENTITY_SERVICE.dto.request.UpdateStaffAddressRequest;
import com.gideonheritagebank.IDENTITY_SERVICE.dto.response.PagedResponse;
import com.gideonheritagebank.IDENTITY_SERVICE.dto.response.StaffAddressResponse;
import com.gideonheritagebank.IDENTITY_SERVICE.entity.BankUser;
import com.gideonheritagebank.IDENTITY_SERVICE.entity.UserAddress;
import com.gideonheritagebank.IDENTITY_SERVICE.enums.UserType;
import com.gideonheritagebank.IDENTITY_SERVICE.exception.BadRequest;
import com.gideonheritagebank.IDENTITY_SERVICE.exception.IdentityNotFoundException;
import com.gideonheritagebank.IDENTITY_SERVICE.exception.ResourceNotFoundException;
import com.gideonheritagebank.IDENTITY_SERVICE.mapper.IdentityMapper;
import com.gideonheritagebank.IDENTITY_SERVICE.repository.BankUserRepository;
import com.gideonheritagebank.IDENTITY_SERVICE.repository.UserAddressRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StaffAddressService {

    private final BankUserRepository bankUserRepository;
    private final UserAddressRepository userAddressRepository;
    private final IdentityMapper identityMapper;

    /* GET STAFF ADDRESSES */
    @PreAuthorize("hasRole('ADMIN')")
    public PagedResponse<StaffAddressResponse> getStaffAddresses(
            UUID staffUserId,
            Pageable pageable
    ) {
        findStaff(staffUserId);

        Page<StaffAddressResponse> responsePage = userAddressRepository
                .findAllByUser_IdOrderByCreatedAtDesc(
                        staffUserId,
                        pageable
                )
                .map(identityMapper::toStaffAddressResponse);

        return PagedResponse.from(responsePage);
    }

    /* ADD STAFF ADDRESS */
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public StaffAddressResponse addAddress(
            UUID staffUserId,
            CreateStaffAddressRequest request
    ) {
        if (request == null) {
            throw new BadRequest("Address request is required");
        }

        BankUser staff = getStaffForUpdate(staffUserId);

        boolean firstAddress = staff.getAddresses().isEmpty();
        boolean makePrimary = firstAddress || Boolean.TRUE.equals(request.primary());

        if (makePrimary) {
            clearPrimaryAddresses(staff);
        }

        UserAddress address = new UserAddress(
                normalizeRequired(request.addressLine1(), "Address line 1"),
                normalizeNullable(request.addressLine2()),
                normalizeRequired(request.city(), "City"),
                normalizeNullable(request.stateOrRegion()),
                normalizeNullable(request.postalCode()),
                normalizeCountryCode(request.countryCode()),
                request.addressType(),
                makePrimary
        );

        staff.addAddress(address);

        bankUserRepository.saveAndFlush(staff);

        return identityMapper.toStaffAddressResponse(address);
    }

    /* UPDATE STAFF ADDRESS */
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public StaffAddressResponse updateAddress(
            UUID staffUserId,
            UUID addressId,
            UpdateStaffAddressRequest request
    ) {
        if (request == null) {
            throw new BadRequest("Address update request is required");
        }

        BankUser staff = getStaffForUpdate(staffUserId);
        UserAddress address = findAddress(addressId, staffUserId);
        boolean wasPrimary = address.isPrimary();
        updateAddressFields(address, request);

        if (Boolean.TRUE.equals(request.primary())) {

            clearPrimaryAddresses(staff);
            address.setPrimary(true);

        } else if (Boolean.FALSE.equals(request.primary())) {

            address.setPrimary(false);

            if (wasPrimary) {
                promoteAnotherAddress(staff, address);
            }
        }

        UserAddress savedAddress = userAddressRepository.saveAndFlush(address);

        return identityMapper.toStaffAddressResponse(savedAddress);
    }

    /* DELETE STAFF ADDRESS */
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteAddress(
            UUID staffUserId,
            UUID addressId
    ) {
        BankUser staff = getStaffForUpdate(staffUserId);
        UserAddress address = findAddress(addressId, staffUserId);
        boolean wasPrimary = address.isPrimary();

        staff.removeAddress(address);

        if (wasPrimary) {
            promoteAnotherAddress(staff, address);
        }

        bankUserRepository.saveAndFlush(staff);
    }

    /*
     * MAKE STAFF ADDRESS PRIMARY
     */
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public StaffAddressResponse makePrimary(
            UUID staffUserId,
            UUID addressId
    ) {
        BankUser staff = getStaffForUpdate(staffUserId);
        UserAddress address = findAddress(addressId, staffUserId);
        clearPrimaryAddresses(staff);
        address.setPrimary(true);

        UserAddress savedAddress = userAddressRepository.saveAndFlush(address);

        return identityMapper.toStaffAddressResponse(savedAddress);
    }

    /* GET STAFF IDENTITY */
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

        validateStaffIdentity(staff);

        return staff;
    }

    /*
     * GET STAFF IDENTITY FOR UPDATE
     *
     * PESSIMISTIC_WRITE prevents concurrent address modifications
     * for the same staff identity.
     */
    private BankUser getStaffForUpdate(UUID staffUserId) {

        if (staffUserId == null) {
            throw new BadRequest("Staff user ID is required");
        }

        BankUser staff = bankUserRepository.findByIdForUpdate(staffUserId)
                .orElseThrow(() ->
                        new IdentityNotFoundException(staffUserId)
                );

        if (staff.getUserType() != UserType.STAFF) {
            throw new BadRequest(
                    "The requested identity is not a staff identity"
            );
        }

        validateStaffIdentity(staff);

        return staff;
    }

    private void validateStaffIdentity(BankUser staff) {

        if (staff.isIdentityClosed()) {
            throw new BadRequest(
                    "Addresses cannot be modified for a closed staff identity"
            );
        }
    }

    /* FIND ADDRESS BELONGING TO THE STAFF USER */
    private UserAddress findAddress(
            UUID addressId,
            UUID staffUserId
    ) {
        if (addressId == null) {
            throw new BadRequest("Address ID is required");
        }

        return userAddressRepository.findByIdAndUser_Id(addressId, staffUserId)
                .orElseThrow(() -> new ResourceNotFoundException(
                                "Address not found: " + addressId
                        )
                );
    }

    /* CLEAR ALL EXISTING PRIMARY ADDRESSES */
    private void clearPrimaryAddresses(BankUser staff) {

        staff.getAddresses()
                .stream()
                .filter(UserAddress::isPrimary)
                .forEach(address -> address.setPrimary(false));
    }

    /*
     * PROMOTE ANOTHER ADDRESS
     * The most recently created remaining address becomes primary.
     */
    private void promoteAnotherAddress(
            BankUser staff,
            UserAddress excludedAddress
    ) {
        staff.getAddresses()
                .stream()
                .filter(address ->
                        !address.getId().equals(excludedAddress.getId())
                )
                .max(
                        Comparator.comparing(
                                UserAddress::getCreatedAt,
                                Comparator.nullsFirst(
                                        Comparator.naturalOrder()
                                )
                        )
                )
                .ifPresent(address -> address.setPrimary(true));
    }

    /* UPDATE ADDRESS FIELDS */
    private void updateAddressFields(
            UserAddress address,
            UpdateStaffAddressRequest request
    ) {
        if (request.addressLine1() != null) {

            address.setAddressLine1(
                    normalizeRequired(
                            request.addressLine1(),
                            "Address line 1"
                    )
            );
        }

        if (request.addressLine2() != null) {

            address.setAddressLine2(normalizeNullable(request.addressLine2()));
        }

        if (request.city() != null) {

            address.setCity(normalizeRequired(request.city(), "City"));
        }

        if (request.stateOrRegion() != null) {

            address.setStateOrRegion(normalizeNullable(request.stateOrRegion()));
        }

        if (request.postalCode() != null) {

            address.setPostalCode(normalizeNullable(request.postalCode()));
        }

        if (request.countryCode() != null) {

            address.setCountryCode(normalizeCountryCode(request.countryCode()));
        }

        if (request.addressType() != null) {
            address.setAddressType(request.addressType());
        }
    }

    private String normalizeRequired(
            String value,
            String fieldName
    ) {

        if (value == null || value.isBlank()) {
            throw new BadRequest(fieldName + " is required");
        }

        return value.trim().replaceAll("\\s+", " ");
    }

    private String normalizeNullable(String value) {

        if (value == null) {
            return null;
        }

        String normalized = value.trim();

        return normalized.isEmpty() ? null : normalized;
    }

    private String normalizeCountryCode(String value) {

        String normalized = normalizeRequired(
                value,
                "Country code"
        );

        if (!normalized.matches("(?i)^[a-z]{2}$")) {
            throw new BadRequest(
                    "Country code must contain exactly two letters"
            );
        }

        return normalized.toUpperCase(java.util.Locale.ROOT);
    }
}
