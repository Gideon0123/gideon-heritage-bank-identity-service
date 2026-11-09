package com.gideonheritagebank.IDENTITY_SERVICE.service;

import com.gideonheritagebank.IDENTITY_SERVICE.dto.request.CreateDepartmentRequest;
import com.gideonheritagebank.IDENTITY_SERVICE.dto.request.UpdateDepartmentRequest;
import com.gideonheritagebank.IDENTITY_SERVICE.dto.response.DepartmentResponse;
import com.gideonheritagebank.IDENTITY_SERVICE.dto.response.PagedResponse;
import com.gideonheritagebank.IDENTITY_SERVICE.entity.Department;
import com.gideonheritagebank.IDENTITY_SERVICE.enums.EmploymentStatus;
import com.gideonheritagebank.IDENTITY_SERVICE.exception.BusinessException;
import com.gideonheritagebank.IDENTITY_SERVICE.mapper.IdentityMapper;
import com.gideonheritagebank.IDENTITY_SERVICE.repository.DepartmentRepository;
import com.gideonheritagebank.IDENTITY_SERVICE.repository.StaffProfileRepository;
import com.gideonheritagebank.IDENTITY_SERVICE.exception.BadRequest;
import com.gideonheritagebank.IDENTITY_SERVICE.exception.ConflictException;
import com.gideonheritagebank.IDENTITY_SERVICE.exception.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final StaffProfileRepository staffProfileRepository;
    private final IdentityMapper identityMapper;
    private static final int MAX_PAGE_SIZE = 100;
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "departmentCode", "name"
    );

    /* CREATE DEPARTMENT */
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public DepartmentResponse createDepartment(
            CreateDepartmentRequest request
    ) {
        if (request == null) {
            throw new BadRequest("Department request is required");
        }

        String departmentCode = normalizeDepartmentCode(request.departmentCode());
        String name = normalizeRequired(request.name());
        String description = normalizeNullable(request.description());

        if (departmentRepository.existsByDepartmentCodeIgnoreCase(departmentCode)) {
            throw new ConflictException(
                    "A department with code " + departmentCode + " already exists"
            );
        }

        Department department = new Department(
                departmentCode,
                name,
                description
        );

        Department savedDepartment = departmentRepository.saveAndFlush(department);

        return identityMapper.toDepartmentResponse(savedDepartment);
    }

    /* GET DEPARTMENT BY ID */
    public DepartmentResponse getDepartmentById(
            UUID departmentId
    ) {
        Department department = findDepartment(departmentId);

        return identityMapper.toDepartmentResponse(department);
    }

    /* GET DEPARTMENTS WITH PAGINATION */
    public PagedResponse<DepartmentResponse> getDepartments(
            Boolean active,
            Pageable pageable
    ) {
        Page<Department> departments;
        Pageable validatedPageable = validateAndBuildPageable(pageable);

        if (active == null) {
            departments = departmentRepository.findAllByOrderByNameAsc(validatedPageable);

        } else {
            departments = departmentRepository.findAllByActive(active, validatedPageable);
        }

        Page<DepartmentResponse> responsePage =
                departments.map(identityMapper::toDepartmentResponse);

        return PagedResponse.from(responsePage);
    }

    /* UPDATE DEPARTMENT */
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public DepartmentResponse updateDepartment(
            UUID departmentId,
            UpdateDepartmentRequest request
    ) {
        if (request == null) {
            throw new BadRequest("Department update request is required");
        }

        Department department = findDepartment(departmentId);

        if (request.name() != null) {

            String name = normalizeRequired(request.name());
            department.setName(name);
        }

        if (request.description() != null) {

            department.setDescription(normalizeNullable(request.description()));
        }

        if (request.active() != null) {

            boolean requestedStatus = request.active();

            /*
             * Do not deactivate a department that still has staff
             * assigned to it in a non-terminal employment state.
             */
            if (!requestedStatus && department.isActive()) {

                validateDepartmentCanBeDeactivated(departmentId);
            }

            department.setActive(requestedStatus);
        }

        Department savedDepartment = departmentRepository.saveAndFlush(department);

        return identityMapper.toDepartmentResponse(savedDepartment);
    }

    /* DEACTIVATE DEPARTMENT */
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public DepartmentResponse deactivateDepartment(
            UUID departmentId
    ) {
        Department department = findDepartment(departmentId);

        if (!department.isActive()) {
            return identityMapper.toDepartmentResponse(department);
        }

        validateDepartmentCanBeDeactivated(departmentId);
        department.setActive(false);

        Department savedDepartment =
                departmentRepository.saveAndFlush(department);

        return identityMapper.toDepartmentResponse(savedDepartment);
    }

    /* ACTIVATE DEPARTMENT */
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public DepartmentResponse activateDepartment(
            UUID departmentId
    ) {
        Department department = findDepartment(departmentId);

        if (!department.isActive()) {

            department.setActive(true);
            departmentRepository.saveAndFlush(department);
        }

        return identityMapper.toDepartmentResponse(department);
    }

    /* INTERNAL DEPARTMENT LOOKUP */
    private Department findDepartment(UUID departmentId) {

        if (departmentId == null) {
            throw new BadRequest("Department ID is required");
        }

        return departmentRepository.findById(departmentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                                "Department not found: " + departmentId
                        )
                );
    }

    /* DEPARTMENT DEACTIVATION VALIDATION */
    private void validateDepartmentCanBeDeactivated(
            UUID departmentId
    ) {
        long assignedStaffCount = 0;

        assignedStaffCount += staffProfileRepository.countByDepartment_IdAndEmploymentStatus(
                departmentId,
                EmploymentStatus.PENDING
        );

        assignedStaffCount += staffProfileRepository.countByDepartment_IdAndEmploymentStatus(
                departmentId,
                EmploymentStatus.ACTIVE
        );

        assignedStaffCount += staffProfileRepository.countByDepartment_IdAndEmploymentStatus(
                departmentId,
                EmploymentStatus.ON_LEAVE
        );

        assignedStaffCount += staffProfileRepository.countByDepartment_IdAndEmploymentStatus(
                departmentId,
                EmploymentStatus.SUSPENDED
        );

        if (assignedStaffCount > 0) {

            throw new ConflictException(
                    "This department cannot be deactivated because "
                            + assignedStaffCount
                            + " staff member(s) are still assigned to it"
            );
        }
    }

    private String normalizeDepartmentCode(String value) {

        if (value == null || value.isBlank()) {
            throw new BadRequest("Department code is required");
        }

        return value.trim().replaceAll("\\s+", "").toUpperCase(Locale.ROOT);
    }

    private String normalizeRequired(
            String value
    ) {
        if (value == null || value.isBlank()) {
            throw new BadRequest("Department name" + " is required");
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

}
