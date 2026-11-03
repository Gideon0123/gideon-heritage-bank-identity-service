package com.gideonheritagebank.IDENTITY_SERVICE.service;

import com.gideonheritagebank.IDENTITY_SERVICE.dto.customer.CreateCustomerAddressRequest;
import com.gideonheritagebank.IDENTITY_SERVICE.dto.customer.CustomerAddressResponse;
import com.gideonheritagebank.IDENTITY_SERVICE.dto.customer.UpdateCustomerAddressRequest;
import com.gideonheritagebank.IDENTITY_SERVICE.entity.BankUser;
import com.gideonheritagebank.IDENTITY_SERVICE.entity.UserAddress;
import com.gideonheritagebank.IDENTITY_SERVICE.enums.UserType;
import com.gideonheritagebank.IDENTITY_SERVICE.exception.BusinessException;
import com.gideonheritagebank.IDENTITY_SERVICE.exception.ResourceNotFoundException;
import com.gideonheritagebank.IDENTITY_SERVICE.repository.BankUserRepository;
import com.gideonheritagebank.IDENTITY_SERVICE.repository.UserAddressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomerAddressService {

    private final BankUserRepository bankUserRepository;
    private final UserAddressRepository userAddressRepository;

    @Transactional(readOnly = true)
    public List<CustomerAddressResponse> getMyAddresses(UUID customerId) {

        BankUser customer = getCustomer(customerId);

        return userAddressRepository
                .findAllByUser_IdOrderByCreatedAtDesc(customer.getId())
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional
    public CustomerAddressResponse addAddress(
            UUID customerId,
            CreateCustomerAddressRequest request
    ) {

        BankUser customer = getCustomerForUpdate(customerId);

        boolean hasExistingAddresses =
                userAddressRepository.existsByUser_Id(customerId);

        boolean makePrimary =
                Boolean.TRUE.equals(request.primary()) || !hasExistingAddresses;

        if (makePrimary) {
            removePrimaryDesignation(customer);
        }

        UserAddress address = new UserAddress(
                normalize(request.addressLine1()),
                normalizeNullable(request.addressLine2()),
                normalize(request.city()),
                normalize(request.stateOrRegion()),
                normalizeNullable(request.postalCode()),
                normalizeCountryCode(request.countryCode()),
                request.addressType(),
                makePrimary
        );

        customer.addAddress(address);

        bankUserRepository.saveAndFlush(customer);

        return mapToResponse(address);
    }

    @Transactional
    public CustomerAddressResponse updateAddress(
            UUID customerId,
            UUID addressId,
            UpdateCustomerAddressRequest request
    ) {

        BankUser customer = getCustomerForUpdate(customerId);

        UserAddress address = getCustomerAddress(customerId, addressId);

        if (request.addressLine1() != null) {
            address.setAddressLine1(normalize(request.addressLine1()));
        }

        if (request.addressLine2() != null) {
            address.setAddressLine2(
                    normalizeNullable(request.addressLine2())
            );
        }

        if (request.city() != null) {
            address.setCity(normalize(request.city()));
        }

        if (request.stateOrRegion() != null) {
            address.setStateOrRegion(normalize(request.stateOrRegion()));
        }

        if (request.postalCode() != null) {
            address.setPostalCode(
                    normalizeNullable(request.postalCode())
            );
        }

        if (request.countryCode() != null) {
            address.setCountryCode(
                    normalizeCountryCode(request.countryCode())
            );
        }

        if (request.addressType() != null) {
            address.setAddressType(request.addressType());
        }

        if (Boolean.TRUE.equals(request.primary())) {

            removePrimaryDesignation(customer);

            address.setPrimary(true);

        } else if (Boolean.FALSE.equals(request.primary())
                && address.isPrimary()) {

            address.setPrimary(false);

            promoteAnotherAddress(customer, address);
        }

        return mapToResponse(userAddressRepository.saveAndFlush(address));
    }

    @Transactional
    public void deleteAddress(UUID customerId, UUID addressId) {

        BankUser customer = getCustomerForUpdate(customerId);

        UserAddress address = getCustomerAddress(customerId, addressId);

        boolean wasPrimary = address.isPrimary();

        customer.removeAddress(address);

        if (wasPrimary) {
            promoteAnotherAddress(customer, address);
        }

        bankUserRepository.saveAndFlush(customer);
    }

    @Transactional
    public CustomerAddressResponse makePrimary(
            UUID customerId,
            UUID addressId
    ) {

        BankUser customer = getCustomerForUpdate(customerId);

        UserAddress address = getCustomerAddress(customerId, addressId);

        removePrimaryDesignation(customer);

        address.setPrimary(true);

        return mapToResponse(userAddressRepository.saveAndFlush(address));
    }

    private BankUser getCustomer(UUID customerId) {

        BankUser customer = bankUserRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Customer identity was not found"
                ));

        validateCustomerType(customer);

        return customer;
    }

    private BankUser getCustomerForUpdate(UUID customerId) {

        BankUser customer = bankUserRepository.findByIdForUpdate(customerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Customer identity was not found"
                ));

        validateCustomerType(customer);

        return customer;
    }

    private void validateCustomerType(BankUser user) {

        if (user.getUserType() != UserType.CUSTOMER) {
            throw new BusinessException(
                    "This operation is only available to customer identities"
            );
        }
    }

    private UserAddress getCustomerAddress(
            UUID customerId,
            UUID addressId
    ) {

        return userAddressRepository
                .findByIdAndUser_Id(addressId, customerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Address was not found"
                ));
    }

    private void removePrimaryDesignation(BankUser customer) {

        customer.getAddresses()
                .stream()
                .filter(UserAddress::isPrimary)
                .forEach(address -> address.setPrimary(false));
    }

    private void promoteAnotherAddress(
            BankUser customer,
            UserAddress excludedAddress
    ) {

        customer.getAddresses()
                .stream()
                .filter(address -> !address.getId().equals(excludedAddress.getId()))
                .findFirst()
                .ifPresent(address -> address.setPrimary(true));
    }

    private String normalize(String value) {
        return value.trim();
    }

    private String normalizeNullable(String value) {

        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }

    private String normalizeCountryCode(String value) {
        return value.trim().toUpperCase();
    }

    private CustomerAddressResponse mapToResponse(UserAddress address) {

        return new CustomerAddressResponse(
                address.getId(),
                address.getAddressLine1(),
                address.getAddressLine2(),
                address.getCity(),
                address.getStateOrRegion(),
                address.getPostalCode(),
                address.getCountryCode(),
                address.getAddressType(),
                address.isPrimary(),
                address.getCreatedAt(),
                address.getUpdatedAt()
        );
    }
}