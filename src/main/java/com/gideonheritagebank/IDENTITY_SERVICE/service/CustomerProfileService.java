package com.gideonheritagebank.IDENTITY_SERVICE.service;

import com.gideonheritagebank.IDENTITY_SERVICE.dto.request.UpdateCustomerProfileRequest;
import com.gideonheritagebank.IDENTITY_SERVICE.dto.response.CustomerProfileResponse;
import com.gideonheritagebank.IDENTITY_SERVICE.entity.BankUser;
import com.gideonheritagebank.IDENTITY_SERVICE.enums.UserType;
import com.gideonheritagebank.IDENTITY_SERVICE.exception.BusinessException;
import com.gideonheritagebank.IDENTITY_SERVICE.exception.ResourceNotFoundException;
import com.gideonheritagebank.IDENTITY_SERVICE.repository.BankUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomerProfileService {

    private final BankUserRepository bankUserRepository;

    @Transactional(readOnly = true)
    public CustomerProfileResponse getMyProfile(UUID customerId) {

        BankUser customer = getCustomer(customerId);
        return mapToResponse(customer);
    }

    @Transactional
    public CustomerProfileResponse updateMyProfile(
            UUID customerId,
            UpdateCustomerProfileRequest request
    ) {
        BankUser customer = getCustomerForUpdate(customerId);

        String firstName = normalizeName(request.firstName());
        String lastName = normalizeName(request.lastName());
        String phoneNumber = normalizePhoneNumber(request.phoneNumber());

        if (bankUserRepository.existsByPhoneNumberAndIdNot(
                phoneNumber,
                customerId
        )) {
            throw new BusinessException(
                    "The supplied phone number is already registered"
            );
        }

        customer.setFirstName(firstName);
        customer.setLastName(lastName);
        customer.setPhoneNumber(phoneNumber);

        String profilePhotoUrl = request.profilePhotoUrl();

        customer.setProfilePhotoUrl(
                profilePhotoUrl == null || profilePhotoUrl.isBlank()
                        ? null : profilePhotoUrl.trim()
        );

        BankUser savedCustomer = bankUserRepository.saveAndFlush(customer);

        return mapToResponse(savedCustomer);
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

    private String normalizeName(String value) {
        return value.trim();
    }

    private String normalizePhoneNumber(String value) {
        return value.trim();
    }

    private CustomerProfileResponse mapToResponse(BankUser user) {

        return new CustomerProfileResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getPhoneNumber(),
                user.getProfilePhotoUrl(),
                user.getUserType(),
                user.isEnabled(),
                user.isEmailVerified(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}