package com.gideonheritagebank.IDENTITY_SERVICE.service;

import com.gideonheritagebank.IDENTITY_SERVICE.dto.request.UpdateCustomerProfileRequest;
import com.gideonheritagebank.IDENTITY_SERVICE.dto.response.CustomerProfileResponse;
import com.gideonheritagebank.IDENTITY_SERVICE.entity.BankUser;
import com.gideonheritagebank.IDENTITY_SERVICE.enums.UserType;
import com.gideonheritagebank.IDENTITY_SERVICE.exception.ResourceNotFoundException;
import com.gideonheritagebank.IDENTITY_SERVICE.exception.BusinessException;
import com.gideonheritagebank.IDENTITY_SERVICE.mapper.IdentityMapper;
import com.gideonheritagebank.IDENTITY_SERVICE.repository.BankUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import com.gideonheritagebank.IDENTITY_SERVICE.cache.CacheNames;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import com.gideonheritagebank.IDENTITY_SERVICE.exception.BadRequest;
import com.gideonheritagebank.IDENTITY_SERVICE.storage.ProfileImageUrlResolver;

@Service
@RequiredArgsConstructor
public class CustomerProfileService {

    private final BankUserRepository bankUserRepository;
    private final IdentityMapper identityMapper;
    private final ProfileImageUrlResolver profileImageUrlResolver;
    @Cacheable(cacheNames = CacheNames.CUSTOMER_PROFILE, key = "T(com.gideonheritagebank.IDENTITY_SERVICE.cache.CacheKeys).customerProfile(#p0)")

    @Transactional(readOnly = true)
    public CustomerProfileResponse getMyProfile(UUID customerId) {

        BankUser customer = getCustomer(customerId);
        return profileImageUrlResolver.resolve(
                identityMapper.toCustomerProfileResponse(customer)
        );
    }
    @Caching(evict = { @CacheEvict(cacheNames = CacheNames.CUSTOMER_PROFILE, key = "T(com.gideonheritagebank.IDENTITY_SERVICE.cache.CacheKeys).customerProfile(#p0)"), @CacheEvict(cacheNames = CacheNames.CUSTOMER_STATUS, key = "T(com.gideonheritagebank.IDENTITY_SERVICE.cache.CacheKeys).customerStatus(#p0)") })

    @Transactional
    public CustomerProfileResponse updateMyProfile(
            UUID customerId,
            UpdateCustomerProfileRequest request
    ) {
        BankUser customer = getCustomerForUpdate(customerId);

        if (request.profilePhotoUrl() != null) {
            throw new BadRequest(
                    "Profile images must be uploaded through POST /api/v1/identity/profile-image."
            );
        }

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

        BankUser savedCustomer = bankUserRepository.saveAndFlush(customer);

        return profileImageUrlResolver.resolve(
                identityMapper.toCustomerProfileResponse(savedCustomer)
        );
    }

    private BankUser getCustomer(UUID customerId) {

        BankUser customer = bankUserRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Customer identity was not found"
                ));

        validateCustomer(customer);

        return customer;
    }

    private BankUser getCustomerForUpdate(UUID customerId) {

        BankUser customer = bankUserRepository.findByIdForUpdate(customerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Customer identity was not found"
                ));

        validateCustomer(customer);

        return customer;
    }

    private void validateCustomer(BankUser user) {

        if (user.getUserType() != UserType.CUSTOMER) {
            throw new ResourceNotFoundException(
                    "Customer identity was not found"
            );
        }

        if (user.isIdentityClosed()) {
            throw new BusinessException(
                    "This customer identity has been closed."
            );
        }
    }

    private String normalizeName(String value) {
        return value.trim();
    }

    private String normalizePhoneNumber(String value) {
        return value.trim();
    }
}