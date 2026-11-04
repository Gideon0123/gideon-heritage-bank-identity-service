package com.gideonheritagebank.IDENTITY_SERVICE.service;

import com.gideonheritagebank.IDENTITY_SERVICE.dto.request.CustomerFilterRequest;
import com.gideonheritagebank.IDENTITY_SERVICE.dto.request.CustomerSearchRequest;
import com.gideonheritagebank.IDENTITY_SERVICE.dto.response.CustomerProfileResponse;
import com.gideonheritagebank.IDENTITY_SERVICE.entity.BankUser;
import com.gideonheritagebank.IDENTITY_SERVICE.enums.UserType;
import com.gideonheritagebank.IDENTITY_SERVICE.exception.BusinessException;
import com.gideonheritagebank.IDENTITY_SERVICE.exception.ResourceNotFoundException;
import com.gideonheritagebank.IDENTITY_SERVICE.mapper.IdentityMapper;
import com.gideonheritagebank.IDENTITY_SERVICE.repository.BankUserRepository;
import com.gideonheritagebank.IDENTITY_SERVICE.specification.CustomerSearchSpecification;
import com.gideonheritagebank.IDENTITY_SERVICE.specification.CustomerSpecification;
import lombok.RequiredArgsConstructor;
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
//        validatePageSize(pageable);
        Pageable validatedPageable = validateAndBuildPageable(pageable);

        Page<BankUser> customers = bankUserRepository.findAll(
                CustomerSearchSpecification.search(request),
                validatedPageable
        );

        return customers.map(identityMapper::toCustomerProfileResponse);
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
}
