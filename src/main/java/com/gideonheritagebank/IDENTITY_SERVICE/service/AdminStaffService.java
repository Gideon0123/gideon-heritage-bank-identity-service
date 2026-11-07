package com.gideonheritagebank.IDENTITY_SERVICE.service;

import com.gideonheritagebank.IDENTITY_SERVICE.dto.request.StaffFilterRequest;
import com.gideonheritagebank.IDENTITY_SERVICE.dto.request.StaffSearchRequest;
import com.gideonheritagebank.IDENTITY_SERVICE.dto.response.StaffResponse;
import com.gideonheritagebank.IDENTITY_SERVICE.entity.BankUser;
import com.gideonheritagebank.IDENTITY_SERVICE.enums.UserType;
import com.gideonheritagebank.IDENTITY_SERVICE.exception.BusinessException;
import com.gideonheritagebank.IDENTITY_SERVICE.exception.ResourceNotFoundException;
import com.gideonheritagebank.IDENTITY_SERVICE.mapper.IdentityMapper;
import com.gideonheritagebank.IDENTITY_SERVICE.repository.BankUserRepository;
import com.gideonheritagebank.IDENTITY_SERVICE.specification.StaffSearchSpecification;
import com.gideonheritagebank.IDENTITY_SERVICE.specification.StaffSpecification;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminStaffService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "firstName",
            "lastName",
            "email",
            "phoneNumber",
            "createdAt",
            "updatedAt",
            "staffProfile.employeeNumber",
            "staffProfile.staffRole",
            "staffProfile.hireDate"
    );

    private final BankUserRepository bankUserRepository;
    private final IdentityMapper identityMapper;


    /* Retrieve one staff member by identity ID. */
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN')")
    public StaffResponse getStaffById(UUID staffId) {

        BankUser staff = bankUserRepository.findByIdAndUserType(staffId, UserType.STAFF)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Staff identity was not found"
                ));

        /*
         * Do not expose an identity as a staff member unless
         * its StaffProfile actually exists.
         */
        if (staff.getStaffProfile() == null) {
            throw new ResourceNotFoundException("Staff profile was not found");
        }

        return identityMapper.toStaffResponse(staff);
    }

    /* Retrieve staff members with filters and pagination. */
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN')")
    public Page<StaffResponse> getStaff(
            StaffFilterRequest filter,
            Pageable pageable
    ) {
        validateFilterDateRanges(filter);
        Pageable validatedPageable = validateAndBuildPageable(pageable);

        Page<BankUser> staffPage = bankUserRepository.findAll(
                StaffSpecification.withFilters(filter),
                validatedPageable
        );

        return staffPage.map(identityMapper::toStaffResponse);
    }


    /* Search staff members. */
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN')")
    public Page<StaffResponse> searchStaff(
            StaffSearchRequest request,
            Pageable pageable
    ) {
        Pageable validatedPageable = validateAndBuildPageable(pageable);

        Page<BankUser> staffPage = bankUserRepository.findAll(
                StaffSearchSpecification.search(request),
                validatedPageable
        );

        return staffPage.map(identityMapper::toStaffResponse);
    }

    /* Validate date ranges before querying the database. */
    private void validateFilterDateRanges(StaffFilterRequest filter) {

        if (filter.getCreatedFrom() != null && filter.getCreatedTo() != null
                && filter.getCreatedFrom().isAfter(filter.getCreatedTo())) {

            throw new BusinessException(
                    "createdFrom cannot be later than createdTo"
            );
        }

        if (filter.getHireDateFrom() != null && filter.getHireDateTo() != null
                && filter.getHireDateFrom().isAfter(filter.getHireDateTo()
        )) {
            throw new BusinessException(
                    "hireDateFrom cannot be later than hireDateTo"
            );
        }
    }

    /*
     * Pagination and sorting validation.
     * API page numbers are one-based:
     * page=1 means the first page.
     * Spring Data internally uses zero-based page numbers.
     */
    private Pageable validateAndBuildPageable(Pageable pageable) {

        int pageNumber = Math.max(pageable.getPageNumber() - 1, 0);
        int pageSize = Math.min(pageable.getPageSize(), MAX_PAGE_SIZE);

        if (pageSize < 1) {
            pageSize = 20;
        }

        Sort sort = validateSort(pageable.getSort());

        return PageRequest.of(pageNumber, pageSize, sort);
    }


    /* Prevent clients from sorting by arbitrary database properties. */
    private Sort validateSort(Sort requestedSort) {

        if (requestedSort.isUnsorted()) {
            return Sort.by(
                    Sort.Order.desc("createdAt")
            );
        }

        for (Sort.Order order : requestedSort) {

            if (!ALLOWED_SORT_FIELDS.contains(order.getProperty())) {

                throw new BusinessException(
                        "Sorting by '" + order.getProperty() + "' is not supported"
                );
            }
        }

        return requestedSort;
    }
}