package com.gideonheritagebank.IDENTITY_SERVICE.service;

import com.gideonheritagebank.IDENTITY_SERVICE.cache.CacheNames;
import com.gideonheritagebank.IDENTITY_SERVICE.dto.request.AssignBranchManagerRequest;
import com.gideonheritagebank.IDENTITY_SERVICE.dto.request.CloseBranchRequest;
import com.gideonheritagebank.IDENTITY_SERVICE.dto.request.CreateBranchRequest;
import com.gideonheritagebank.IDENTITY_SERVICE.dto.request.UpdateBranchRequest;
import com.gideonheritagebank.IDENTITY_SERVICE.dto.response.BranchResponse;
import com.gideonheritagebank.IDENTITY_SERVICE.dto.response.PagedResponse;
import com.gideonheritagebank.IDENTITY_SERVICE.entity.BankUser;
import com.gideonheritagebank.IDENTITY_SERVICE.entity.Branch;
import com.gideonheritagebank.IDENTITY_SERVICE.entity.StaffProfile;
import com.gideonheritagebank.IDENTITY_SERVICE.enums.BranchStatus;
import com.gideonheritagebank.IDENTITY_SERVICE.enums.EmploymentStatus;
import com.gideonheritagebank.IDENTITY_SERVICE.enums.StaffRole;
import com.gideonheritagebank.IDENTITY_SERVICE.enums.UserType;
import com.gideonheritagebank.IDENTITY_SERVICE.exception.BadRequest;
import com.gideonheritagebank.IDENTITY_SERVICE.exception.BusinessException;
import com.gideonheritagebank.IDENTITY_SERVICE.exception.ConflictException;
import com.gideonheritagebank.IDENTITY_SERVICE.exception.ResourceNotFoundException;
import com.gideonheritagebank.IDENTITY_SERVICE.mapper.IdentityMapper;
import com.gideonheritagebank.IDENTITY_SERVICE.repository.BankUserRepository;
import com.gideonheritagebank.IDENTITY_SERVICE.repository.BranchRepository;
import com.gideonheritagebank.IDENTITY_SERVICE.repository.StaffProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@PreAuthorize("hasRole('ADMIN')")
public class BranchService {

    private static final int MAX_PAGE_SIZE = 100;

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "branchCode",
            "name",
            "branchType",
            "city",
            "stateOrRegion",
            "status",
            "openedDate",
            "createdAt"
    );

    private static final Set<EmploymentStatus> ACTIVE_ASSIGNMENT_STATUSES =
            Set.of(
                    EmploymentStatus.PENDING,
                    EmploymentStatus.ACTIVE,
                    EmploymentStatus.ON_LEAVE,
                    EmploymentStatus.SUSPENDED
            );

    private final BranchRepository branchRepository;
    private final BankUserRepository bankUserRepository;
    private final StaffProfileRepository staffProfileRepository;
    private final IdentityMapper identityMapper;

    // CREATE BRANCH
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.BRANCHES, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.STAFF_PROFILE, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.STAFF_DIRECTORY, allEntries = true)
    })
    public BranchResponse createBranch(
            CreateBranchRequest request
    ) {
        validateCreateRequest(request);

        String branchCode = normalizeBranchCode(request.branchCode());

        if (branchRepository.existsByBranchCodeIgnoreCase(branchCode)) {
            throw new ConflictException("A branch with code " + branchCode + " already exists.");
        }

        LocalDate openedDate = request.openedDate();

        if (openedDate.isAfter(LocalDate.now())) {
            throw new BadRequest("Branch opening date cannot be in the future.");
        }

        Branch branch = Branch.create(
                branchCode,
                normalizeRequired(request.name(), "Branch name"),
                request.branchType(),
                normalizeRequired(request.addressLine1(), "Address line 1"),
                normalizeNullable(request.addressLine2()),
                normalizeRequired(request.city(), "City"),
                normalizeRequired(request.stateOrRegion(), "State or region"),
                normalizeNullable(request.postalCode()),
                normalizeCountryCode(request.countryCode()),
                normalizeNullable(request.phoneNumber()),
                normalizeEmail(request.email()),
                openedDate
        );

        Branch savedBranch = saveBranch(branch);

        return identityMapper.toBranchResponse(savedBranch);
    }

    // GET BRANCH BY ID
    @Cacheable(
            cacheNames = CacheNames.BRANCHES, key = "'id:' + #p0"
    )
    public BranchResponse getBranchById(UUID branchId) {

        Branch branch = findBranch(branchId);
        return identityMapper.toBranchResponse(branch);
    }

    // GET BRANCH BY CODE
    @Cacheable(
            cacheNames = CacheNames.BRANCHES,
            key = "'code:' + #p0"
    )
    public BranchResponse getBranchByCode(String branchCode) {

        String normalizedCode = normalizeBranchCode(branchCode);

        Branch branch = branchRepository
                .findWithManagerByBranchCodeIgnoreCase(normalizedCode)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Branch not found with code: " + normalizedCode
                ));

        return identityMapper.toBranchResponse(branch);
    }

    // GET BRANCHES WITH PAGINATION AND SEARCH
    @Cacheable(
            cacheNames = CacheNames.BRANCHES,
            key = "'search:' + #p0 + ':status:' + #p1 + ':city:' + #p2"
                    + " + ':page:' + #p3.pageNumber"
                    + " + ':size:' + #p3.pageSize"
                    + " + ':sort:' + #p3.sort.toString()"
    )
    public PagedResponse<BranchResponse> getBranches(
            String keyword,
            BranchStatus status,
            String city,
            Pageable pageable
    ) {

        String normalizedKeyword = normalizeNullable(keyword);
        String normalizedCity = normalizeNullable(city);

        Pageable validatedPageable = validateAndBuildPageable(pageable);

        Page<Branch> branches = branchRepository.searchBranches(
                normalizedKeyword,
                status,
                normalizedCity,
                validatedPageable
        );

        Page<BranchResponse> responsePage = branches.map(
                identityMapper::toBranchResponse
        );

        return PagedResponse.from(responsePage);
    }

    // UPDATE BRANCH
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.BRANCHES, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.STAFF_PROFILE, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.STAFF_DIRECTORY, allEntries = true)
    })
    public BranchResponse updateBranch(
            UUID branchId,
            UpdateBranchRequest request
    ) {

        if (request == null) {
            throw new BadRequest("Branch update request is required.");
        }

        Branch branch = findBranchForUpdate(branchId);

        if (branch.getStatus() == BranchStatus.CLOSED) {
            throw new BadRequest("A permanently closed branch cannot be updated.");
        }

        if (request.name() != null) {
            branch.setName(normalizeRequired(request.name(), "Branch name"));
        }

        if (request.branchType() != null) {
            branch.setBranchType(request.branchType());
        }

        if (request.addressLine1() != null) {
            branch.setAddressLine1(
                    normalizeRequired(request.addressLine1(), "Address line 1")
            );
        }

        if (request.addressLine2() != null) {
            branch.setAddressLine2(normalizeNullable(request.addressLine2()));
        }

        if (request.city() != null) {
            branch.setCity(normalizeRequired(request.city(), "City"));
        }

        if (request.stateOrRegion() != null) {
            branch.setStateOrRegion(
                    normalizeRequired(request.stateOrRegion(), "State or region")
            );
        }

        if (request.postalCode() != null) {
            branch.setPostalCode(normalizeNullable(request.postalCode()));
        }

        if (request.countryCode() != null) {
            branch.setCountryCode(normalizeCountryCode(request.countryCode()));
        }

        if (request.phoneNumber() != null) {
            branch.setPhoneNumber(normalizeNullable(request.phoneNumber()));
        }

        if (request.email() != null) {
            branch.setEmail(normalizeEmail(request.email()));
        }

        Branch savedBranch = saveBranch(branch);

        return identityMapper.toBranchResponse(savedBranch);
    }

    // ASSIGN BRANCH MANAGER
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.BRANCHES, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.STAFF_PROFILE, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.STAFF_DIRECTORY, allEntries = true)
    })
    public BranchResponse assignBranchManager(
            UUID branchId,
            AssignBranchManagerRequest request
    ) {
        if (request == null || request.branchManagerId() == null) {
            throw new BadRequest("Branch manager assignment is required.");
        }

        Branch branch = findBranchForUpdate(branchId);

        ensureBranchIsActive(branch);

        UUID managerId = request.branchManagerId();

        BankUser manager = bankUserRepository
                .findByIdAndUserType(managerId, UserType.STAFF)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Staff identity not found: " + managerId
                ));

        StaffProfile managerProfile = validateBranchManager(manager, branch);

        if (branch.getBranchManager() != null
                && branch.getBranchManager().getId().equals(managerId)) {

            return identityMapper.toBranchResponse(branch);
        }

        if (branchRepository.existsByBranchManager_IdAndIdNot(
                managerId,
                branchId
        )) {
            throw new ConflictException(
                    "This staff member is already the manager of another branch."
            );
        }

        branch.assignBranchManager(manager);

        Branch savedBranch = saveBranch(branch);

        return identityMapper.toBranchResponse(savedBranch);
    }

    // REMOVE BRANCH MANAGER
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.BRANCHES, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.STAFF_PROFILE, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.STAFF_DIRECTORY, allEntries = true)
    })
    public BranchResponse removeBranchManager(UUID branchId) {

        Branch branch = findBranchForUpdate(branchId);

        if (branch.getStatus() == BranchStatus.CLOSED) {
            throw new BadRequest(
                    "The manager of a closed branch cannot be changed."
            );
        }

        if (branch.getBranchManager() == null) {
            return identityMapper.toBranchResponse(branch);
        }

        branch.removeBranchManager();

        Branch savedBranch = saveBranch(branch);

        return identityMapper.toBranchResponse(savedBranch);
    }

    // ACTIVATE BRANCH
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.BRANCHES, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.STAFF_PROFILE, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.STAFF_DIRECTORY, allEntries = true)
    })
    public BranchResponse activateBranch(UUID branchId) {

        Branch branch = findBranchForUpdate(branchId);

        if (branch.getStatus() == BranchStatus.CLOSED) {
            throw new BadRequest(
                    "A permanently closed branch cannot be reactivated."
            );
        }

        branch.activate();

        Branch savedBranch = saveBranch(branch);

        return identityMapper.toBranchResponse(savedBranch);
    }

    // DEACTIVATE BRANCH
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.BRANCHES, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.STAFF_PROFILE, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.STAFF_DIRECTORY, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.STAFF_EMPLOYMENT, allEntries = true)
    })
    public BranchResponse deactivateBranch(UUID branchId) {

        Branch branch = findBranchForUpdate(branchId);

        if (branch.getStatus() == BranchStatus.CLOSED) {
            throw new BadRequest(
                    "A permanently closed branch cannot be deactivated."
            );
        }

        if (branch.getStatus() == BranchStatus.INACTIVE) {
            return identityMapper.toBranchResponse(branch);
        }

        validateBranchHasNoActiveStaff(branchId);

        branch.deactivate();

        Branch savedBranch = saveBranch(branch);

        return identityMapper.toBranchResponse(savedBranch);
    }

    // PERMANENTLY CLOSE BRANCH
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.BRANCHES, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.STAFF_PROFILE, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.STAFF_DIRECTORY, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.STAFF_EMPLOYMENT, allEntries = true)
    })
    public BranchResponse closeBranch(
            UUID branchId,
            CloseBranchRequest request
    ) {

        if (request == null || request.reason() == null
                || request.reason().isBlank()) {

            throw new BadRequest("A branch closure reason is required.");
        }

        String reason = request.reason().trim();

        if (reason.length() > 1000) {
            throw new BadRequest(
                    "Branch closure reason cannot exceed 1000 characters."
            );
        }

        Branch branch = findBranchForUpdate(branchId);

        if (branch.getStatus() == BranchStatus.CLOSED) {
            throw new ConflictException(
                    "This branch has already been permanently closed."
            );
        }

        validateBranchHasNoActiveStaff(branchId);

        branch.close(Instant.now(), reason);
        branch.removeBranchManager();

        Branch savedBranch = saveBranch(branch);

        return identityMapper.toBranchResponse(savedBranch);
    }


    // BRANCH MANAGER VALIDATION
    private StaffProfile validateBranchManager(
            BankUser manager,
            Branch branch
    ) {

        if (manager.getUserType() != UserType.STAFF) {
            throw new BadRequest("A branch manager must be a staff identity.");
        }

        if (manager.isIdentityClosed()
                || manager.isSuspended()
                || !manager.isEnabled()
                || !manager.isEmailVerified()
                || !manager.isAccountNonLocked()) {

            throw new BadRequest(
                    "The selected branch manager does not have an active staff identity."
            );
        }

        StaffProfile profile = staffProfileRepository.findByUser_Id(manager.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Staff profile not found for the selected branch manager."
                ));

        if (profile.getStaffRole() != StaffRole.BRANCH_MANAGER) {
            throw new BadRequest(
                    "The selected staff member must have the BRANCH_MANAGER role."
            );
        }

        if (profile.getEmploymentStatus() != EmploymentStatus.ACTIVE) {
            throw new BadRequest("A branch manager must have ACTIVE employment status.");
        }

        if (profile.getBranch() == null || !profile.getBranch().getId().equals(branch.getId())) {

            throw new BadRequest(
                    "The selected branch manager must first be assigned to this branch through staff employment management."
            );
        }

        return profile;
    }

    // BRANCH ASSIGNMENT VALIDATION
    private void validateBranchHasNoActiveStaff(UUID branchId) {

        long assignedStaffCount = 0;

        for (EmploymentStatus status : ACTIVE_ASSIGNMENT_STATUSES) {

            assignedStaffCount +=
                    staffProfileRepository.countByBranch_IdAndEmploymentStatus(
                            branchId,
                            status
                    );
        }

        if (assignedStaffCount > 0) {
            throw new ConflictException(
                    "This branch cannot be deactivated or closed because "
                            + assignedStaffCount
                            + " staff member(s) are still assigned to it. "
                            + "Transfer or terminate their employment first."
            );
        }
    }

    // INTERNAL BRANCH LOOKUPS
    private Branch findBranch(UUID branchId) {

        if (branchId == null) {
            throw new BadRequest("Branch ID is required.");
        }

        return branchRepository.findWithManagerById(branchId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Branch not found: " + branchId
                ));
    }

    private Branch findBranchForUpdate(UUID branchId) {

        if (branchId == null) {
            throw new BadRequest("Branch ID is required.");
        }

        Branch branch = branchRepository.findByIdForUpdate(branchId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Branch not found: " + branchId
                ));

        /*
         * Initialize the manager relationship while inside the
         * transaction. The response mapper may need these fields.
         */
        if (branch.getBranchManager() != null) {
            branch.getBranchManager().getFirstName();
        }

        return branch;
    }

    private void ensureBranchIsActive(Branch branch) {

        if (branch.getStatus() != BranchStatus.ACTIVE) {
            throw new BadRequest("A branch manager can only be assigned to an active branch.");
        }
    }

    // PERSISTENCE
    private Branch saveBranch(Branch branch) {

        try {
            return branchRepository.saveAndFlush(branch);

        } catch (DataIntegrityViolationException exception) {

            throw new ConflictException(
                    "The branch could not be saved because its branch code "
                            + "or branch-manager assignment conflicts with an existing record."
            );
        }
    }

    // REQUEST VALIDATION
    private void validateCreateRequest(CreateBranchRequest request) {

        if (request == null) {
            throw new BadRequest("Branch creation request is required.");
        }
    }

    // NORMALIZATION
    private String normalizeBranchCode(String value) {

        if (value == null || value.isBlank()) {
            throw new BadRequest("Branch code is required.");
        }

        return value.trim()
                .replaceAll("\\s+", "")
                .toUpperCase(Locale.ROOT);
    }

    private String normalizeRequired(
            String value,
            String fieldName
    ) {

        if (value == null || value.isBlank()) {
            throw new BadRequest(fieldName + " is required.");
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

        if (value == null || !value.trim().matches("^[A-Za-z]{2}$")) {
            throw new BadRequest("Country code must contain exactly two letters.");
        }

        return value.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeEmail(String value) {

        String normalized = normalizeNullable(value);

        return normalized == null
                ? null : normalized.toLowerCase(Locale.ROOT);
    }

    // PAGINATION AND SORT VALIDATION
    private Pageable validateAndBuildPageable(Pageable pageable) {

        if (pageable == null) {
            throw new BadRequest("Pagination information is required.");
        }

        /*
         * API pagination is 1-based.
         *
         * Page 1 -> Spring Data page 0
         * Page 2 -> Spring Data page 1
         * Page 3 -> Spring Data page 2
         */
        if (pageable.getPageNumber() < 1) {
            throw new BadRequest("Page number must start from 1.");
        }

        if (pageable.getPageSize() < 1) {
            throw new BadRequest("Page size must be greater than zero.");
        }

        int zeroBasedPage = pageable.getPageNumber() - 1;
        int pageSize = Math.min(pageable.getPageSize(), MAX_PAGE_SIZE);
        Sort sort = validateSort(pageable.getSort());
        return PageRequest.of(zeroBasedPage, pageSize, sort);
    }

    private Sort validateSort(Sort requestedSort) {

        if (requestedSort.isUnsorted()) {
            return Sort.by(
                    Sort.Order.asc("branchCode"),
                    Sort.Order.asc("id")
            );
        }

        for (Sort.Order order : requestedSort) {

            if (!ALLOWED_SORT_FIELDS.contains(order.getProperty())) {
                throw new BusinessException(
                        "Sorting by '"
                                + order.getProperty()
                                + "' is not supported for branches."
                );
            }
        }

        Sort validatedSort = requestedSort;

        /*
         * Always add a deterministic tie-breaker.
         * This prevents records with identical sort values from
         * shifting unpredictably between pages.
         */
        if (requestedSort.getOrderFor("id") == null) {
            validatedSort = validatedSort.and(
                    Sort.by(Sort.Order.asc("id"))
            );
        }

        return validatedSort;
    }
}

