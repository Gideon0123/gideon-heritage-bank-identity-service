package com.gideonheritagebank.IDENTITY_SERVICE.service;

import com.gideonheritagebank.IDENTITY_SERVICE.dto.request.AdminUpdateCustomerRequest;
import com.gideonheritagebank.IDENTITY_SERVICE.dto.request.CustomerFilterRequest;
import com.gideonheritagebank.IDENTITY_SERVICE.dto.request.CustomerSearchRequest;
import com.gideonheritagebank.IDENTITY_SERVICE.dto.response.CustomerProfileResponse;
import com.gideonheritagebank.IDENTITY_SERVICE.entity.BankUser;
import com.gideonheritagebank.IDENTITY_SERVICE.enums.UserType;
import com.gideonheritagebank.IDENTITY_SERVICE.exception.*;
import com.gideonheritagebank.IDENTITY_SERVICE.mapper.IdentityMapper;
import com.gideonheritagebank.IDENTITY_SERVICE.repository.BankUserRepository;
import com.gideonheritagebank.IDENTITY_SERVICE.specification.CustomerSearchSpecification;
import com.gideonheritagebank.IDENTITY_SERVICE.specification.CustomerSpecification;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminCustomerService {

    private final BankUserRepository bankUserRepository;
    private final IdentityMapper identityMapper;
    private static final int MAX_PAGE_SIZE = 100;
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "firstName",
            "lastName",
            "email",
            "phoneNumber",
            "createdAt",
            "updatedAt"
    );

    @Transactional(readOnly = true)
    public Page<CustomerProfileResponse> getCustomers(
            CustomerFilterRequest filter,
            Pageable pageable
    ) {
        validateDateRange(filter);
        Pageable validatedPageable = validateAndBuildPageable(pageable);

        Page<BankUser> customers = bankUserRepository.findAll(
                CustomerSpecification.withFilters(filter),
                validatedPageable
        );

        return customers.map(identityMapper::toCustomerProfileResponse);
    }

    @Transactional(readOnly = true)
    public CustomerProfileResponse getCustomerById(UUID customerId) {

        BankUser customer = bankUserRepository
                .findByIdAndUserType(customerId, UserType.CUSTOMER)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Customer identity was not found"
                ));

        return identityMapper.toCustomerProfileResponse(customer);
    }

    @Transactional(readOnly = true)
    public Page<CustomerProfileResponse> searchCustomers(
            CustomerSearchRequest request,
            Pageable pageable
    ) {
        Pageable validatedPageable = validateAndBuildPageable(pageable);

        Page<BankUser> customers = bankUserRepository.findAll(
                CustomerSearchSpecification.search(request),
                validatedPageable
        );

        return customers.map(identityMapper::toCustomerProfileResponse);
    }

    @Transactional
    public CustomerProfileResponse updateCustomerByStaff(
            UUID customerId,
            AdminUpdateCustomerRequest request
    ) {
        normalizeReason(request.reason());

        BankUser customer = getCustomerForUpdate(customerId);
        String firstName = normalizeName(request.firstName());
        String lastName = normalizeName(request.lastName());
        String phoneNumber = getPhoneNumber(request, firstName, lastName);

        if (bankUserRepository.existsByPhoneNumberAndIdNot(
                phoneNumber,
                customerId
        )) {
            throw new PhoneNumberAlreadyRegisteredException();
        }

        customer.setFirstName(firstName);
        customer.setLastName(lastName);
        customer.setPhoneNumber(phoneNumber);

        String photoUrl = request.profilePhotoUrl();

        customer.setProfilePhotoUrl(
                photoUrl == null || photoUrl.isBlank()
                        ? null : photoUrl.trim()
        );

        BankUser savedCustomer = bankUserRepository.saveAndFlush(customer);

        return identityMapper.toCustomerProfileResponse(savedCustomer);
    }

    private @NonNull String getPhoneNumber(
            AdminUpdateCustomerRequest request,
            String firstName,
            String lastName
    ) {
        String phoneNumber = normalizePhoneNumber(request.phoneNumber());

        if (firstName.isBlank()) {
            throw new InvalidIdentityStateException("First name is required.");
        }

        if (lastName.isBlank()) {
            throw new InvalidIdentityStateException("Last name is required.");
        }

        if (phoneNumber.isBlank()) {
            throw new InvalidIdentityStateException("Phone number is required.");
        }
        return phoneNumber;
    }

    private Pageable validateAndBuildPageable(Pageable pageable) {

        int pageNumber = Math.max(pageable.getPageNumber() - 1, 0);
        int pageSize = Math.min(pageable.getPageSize(), MAX_PAGE_SIZE);

        Sort sort = validateSort(pageable.getSort());

        return PageRequest.of(pageNumber, pageSize, sort);
    }

    private Sort validateSort(Sort requestedSort) {

        if (requestedSort.isUnsorted()) {
            return Sort.by(
                    Sort.Order.desc("createdAt")
            );
        }

        for (Sort.Order order : requestedSort) {

            if (!ALLOWED_SORT_FIELDS.contains(order.getProperty())) {

                throw new BusinessException(
                        "Sorting by '" + order.getProperty()
                                + "' is not supported"
                );
            }
        }

        return requestedSort;
    }

    private void validateDateRange(CustomerFilterRequest filter) {

        if (filter.getCreatedFrom() != null
                && filter.getCreatedTo() != null
                && filter.getCreatedFrom().isAfter(filter.getCreatedTo())) {

            throw new BusinessException(
                    "createdFrom cannot be later than createdTo"
            );
        }
    }

    private BankUser getCustomerForUpdate(UUID customerId) {

        BankUser customer = bankUserRepository.findByIdForUpdate(customerId)
                .orElseThrow(() -> new IdentityNotFoundException(customerId));

        if (customer.getUserType() != UserType.CUSTOMER) {
            throw new IdentityNotFoundException(customerId);
        }

        return customer;
    }

    private void normalizeReason(String reason) {

        if (reason == null || reason.isBlank()) {
            throw new InvalidIdentityStateException(
                    "A reason must be provided for this identity operation."
            );
        }

        String normalized = reason.trim();

        if (normalized.length() < 5 || normalized.length() > 1000) {
            throw new InvalidIdentityStateException(
                    "Reason must contain between 5 and 1000 characters."
            );
        }

    }

    private String normalizeName(String value) {
        if (value == null) {return "";}

        return value.trim().replaceAll("\\s+", " ");
    }

    private String normalizePhoneNumber(String phoneNumber) {
        if (phoneNumber == null) {
            return "";
        }

        return phoneNumber.trim();
    }
}
