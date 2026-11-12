package com.gideonheritagebank.IDENTITY_SERVICE.service;

import com.gideonheritagebank.IDENTITY_SERVICE.dto.request.UpdateStaffEmploymentRequest;
import com.gideonheritagebank.IDENTITY_SERVICE.dto.response.PagedResponse;
import com.gideonheritagebank.IDENTITY_SERVICE.dto.response.StaffEmploymentHistoryResponse;
import com.gideonheritagebank.IDENTITY_SERVICE.entity.BankUser;
import com.gideonheritagebank.IDENTITY_SERVICE.entity.Department;
import com.gideonheritagebank.IDENTITY_SERVICE.entity.StaffEmploymentHistory;
import com.gideonheritagebank.IDENTITY_SERVICE.entity.StaffProfile;
import com.gideonheritagebank.IDENTITY_SERVICE.enums.EmploymentStatus;
import com.gideonheritagebank.IDENTITY_SERVICE.enums.UserType;
import com.gideonheritagebank.IDENTITY_SERVICE.exception.BadRequest;
import com.gideonheritagebank.IDENTITY_SERVICE.exception.IdentityNotFoundException;
import com.gideonheritagebank.IDENTITY_SERVICE.exception.ResourceNotFoundException;
import com.gideonheritagebank.IDENTITY_SERVICE.mapper.IdentityMapper;
import com.gideonheritagebank.IDENTITY_SERVICE.repository.BankUserRepository;
import com.gideonheritagebank.IDENTITY_SERVICE.repository.DepartmentRepository;
import com.gideonheritagebank.IDENTITY_SERVICE.repository.StaffEmploymentHistoryRepository;
import com.gideonheritagebank.IDENTITY_SERVICE.repository.StaffProfileRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StaffEmploymentService {

    private final StaffProfileRepository staffProfileRepository;
    private final StaffEmploymentHistoryRepository employmentHistoryRepository;
    private final DepartmentRepository departmentRepository;
    private final BankUserRepository bankUserRepository;
    private final IdentityMapper identityMapper;
    private static final int MAX_PAGE_SIZE = 100;

    /*
     * UPDATE STAFF EMPLOYMENT
     *
     * changedByUserId is supplied by the controller using the
     * authenticated administrator's identity.
     *
     * It must never be taken from the request body.
     */
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public StaffEmploymentHistoryResponse updateEmployment(
            UUID staffProfileId,
            UpdateStaffEmploymentRequest request,
            UUID changedByUserId
    ) {
        validateRequest(request);

        StaffProfile profile = staffProfileRepository.findByIdForUpdate(staffProfileId)
                .orElseThrow(() -> new ResourceNotFoundException(
                                "Staff profile not found: " + staffProfileId
                        )
                );

        BankUser staffUser = profile.getUser();

        if (staffUser == null || staffUser.getUserType() != UserType.STAFF) {

            throw new BadRequest("Employment can only be updated for a staff identity");
        }

        if (staffUser.isIdentityClosed()) {
            throw new BadRequest(
                    "Employment cannot be updated for a closed identity"
            );
        }

        if (profile.getEmploymentStatus() == EmploymentStatus.TERMINATED) {
            throw new BadRequest(
                    "A terminated staff member's employment cannot be modified"
            );
        }

        LocalDate effectiveFrom = request.effectiveFrom();
        validateEffectiveDate(profile, effectiveFrom);
        validateEmploymentEndDate(request);
        Department department = resolveDepartment(request.departmentId());
        validateDepartmentAssignment(department);
        validateReportingManager(request.reportingManagerId(), staffUser.getId());

        StaffEmploymentHistory currentHistory = employmentHistoryRepository
                .findFirstByStaffProfile_IdAndEffectiveToIsNull(
                        staffProfileId
                )
                .orElseThrow(() -> new BadRequest(
                                "Current employment history record is missing"
                        )
                );

        /*
         * Close the previous employment-history record.
         * effectiveTo is inclusive, so the previous assignment
         * ends one day before the new assignment begins.
         */
        currentHistory.setEffectiveTo(effectiveFrom.minusDays(1));

        employmentHistoryRepository.saveAndFlush(currentHistory);

        /* Update the current staff profile. */
        profile.setDepartment(department);
        profile.setBranchId(request.branchId());
        profile.setJobTitle(normalizeNullable(request.jobTitle()));
        profile.setEmploymentType(request.employmentType());
        profile.setEmploymentStatus(request.employmentStatus());
        profile.setAssignmentStartDate(effectiveFrom);
        profile.setEmploymentEndDate(request.employmentEndDate());
        profile.setReportingManagerId(request.reportingManagerId());

        staffProfileRepository.save(profile);

        /* CREATE NEW EMPLOYMENT HISTORY */
        StaffEmploymentHistory newHistory = new StaffEmploymentHistory(
                profile,
                department,
                request.branchId(),
                normalizeNullable(request.jobTitle()),
                request.employmentType(),
                request.employmentStatus(),
                effectiveFrom,
                normalizeNullable(request.changeReason()),
                changedByUserId
        );

        /*
         * A terminated employment record is terminal.
         * It therefore has an effectiveTo date instead of
         * remaining an open-ended current record.
         */
        if (request.employmentStatus() == EmploymentStatus.TERMINATED) {

            newHistory.setEffectiveTo(request.employmentEndDate());
        }

        /*
         * The constructor already associates the history record
         * with its StaffProfile.
         *
         * Save directly through the history repository.
         */
        StaffEmploymentHistory savedHistory = employmentHistoryRepository.saveAndFlush(newHistory);

        return identityMapper.toStaffEmploymentHistoryResponse(savedHistory);
    }

    /* GET STAFF EMPLOYMENT HISTORY */
    public PagedResponse<StaffEmploymentHistoryResponse> getEmploymentHistory(
            UUID staffProfileId,
            Pageable pageable
    ) {
        if (!staffProfileRepository.existsById(staffProfileId)) {
            throw new ResourceNotFoundException(
                    "Staff profile not found: " + staffProfileId
            );
        }

        Pageable validatedPageable = validateAndBuildPageable(pageable);

        Page<StaffEmploymentHistoryResponse> responsePage = employmentHistoryRepository
                .findAllByStaffProfile_IdOrderByEffectiveFromDescCreatedAtDesc(
                        staffProfileId,
                        validatedPageable
                )
                .map(identityMapper::toStaffEmploymentHistoryResponse);

        return PagedResponse.from(responsePage);
    }

    /* GET CURRENT EMPLOYMENT HISTORY */
    public StaffEmploymentHistoryResponse getCurrentEmployment(
            UUID staffProfileId
    ) {
        if (!staffProfileRepository.existsById(staffProfileId)) {
            throw new ResourceNotFoundException(
                    "Staff profile not found: " + staffProfileId
            );
        }

        StaffEmploymentHistory history = employmentHistoryRepository
                .findFirstByStaffProfile_IdAndEffectiveToIsNull(
                        staffProfileId
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Current employment history not found for staff profile: "
                                        + staffProfileId
                        )
                );

        return identityMapper.toStaffEmploymentHistoryResponse(history);
    }

    /* VALIDATE REQUEST */
    private void validateRequest(
            UpdateStaffEmploymentRequest request
    ) {
        if (request == null) {
            throw new BadRequest("Employment update request is required");
        }

        if (request.employmentStatus() == null) {
            throw new BadRequest("Employment status is required");
        }

        if (request.effectiveFrom() == null) {
            throw new BadRequest("Employment effective date is required");
        }
    }

    /* VALIDATE EFFECTIVE DATE */
    private void validateEffectiveDate(
            StaffProfile profile,
            LocalDate effectiveFrom
    ) {
        if (effectiveFrom.isAfter(LocalDate.now())) {
            throw new BadRequest(
                    "Future-dated employment changes are not supported"
            );
        }

        LocalDate currentStartDate = profile.getAssignmentStartDate();

        if (currentStartDate != null && !effectiveFrom.isAfter(currentStartDate)) {

            throw new BadRequest(
                    "The new effective date must be after the current assignment start date"
            );
        }
    }

    /* VALIDATE EMPLOYMENT END DATE */
    private void validateEmploymentEndDate(
            UpdateStaffEmploymentRequest request
    ) {
        if (request.employmentStatus() == EmploymentStatus.TERMINATED) {

            if (request.employmentEndDate() == null) {
                throw new BadRequest(
                        "Employment end date is required for terminated staff"
                );
            }

            if (request.employmentEndDate().isBefore(request.effectiveFrom())) {
                throw new BadRequest(
                        "Employment end date cannot be before the effective date"
                );
            }

            if (request.employmentEndDate().isAfter(LocalDate.now())) {
                throw new BadRequest(
                        "Future employment termination dates are not supported"
                );
            }

        } else if (request.employmentEndDate() != null) {

            throw new BadRequest(
                    "Employment end date is only allowed for terminated staff"
            );
        }
    }

    /* RESOLVE DEPARTMENT */
    private Department resolveDepartment(UUID departmentId) {

        if (departmentId == null) {
            return null;
        }

        return departmentRepository.findById(departmentId).orElseThrow(() ->
                new ResourceNotFoundException(
                        "Department not found: " + departmentId
                )
        );
    }

    /* VALIDATE DEPARTMENT */
    private void validateDepartmentAssignment(Department department) {

        if (department != null && !department.isActive()) {
            throw new BadRequest(
                    "Staff cannot be assigned to an inactive department"
            );
        }
    }

    /* VALIDATE REPORTING MANAGER */
    private void validateReportingManager(
            UUID reportingManagerId,
            UUID staffUserId
    ) {
        if (reportingManagerId == null) {
            return;
        }

        if (reportingManagerId.equals(staffUserId)) {
            throw new BadRequest(
                    "A staff member cannot report to themselves"
            );
        }

        BankUser manager = bankUserRepository.findByIdAndUserType(
                        reportingManagerId,
                        UserType.STAFF
                )
                .orElseThrow(() ->
                        new IdentityNotFoundException(reportingManagerId)
                );

        if (manager.isIdentityClosed() || manager.isSuspended() || !manager.isEnabled()) {

            throw new BadRequest(
                    "The selected reporting manager is not an active staff identity"
            );
        }

        StaffProfile managerProfile = staffProfileRepository.findByUser_Id(reportingManagerId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Reporting manager staff profile not found"
                        )
                );

        if (managerProfile.getEmploymentStatus() == EmploymentStatus.TERMINATED) {

            throw new BadRequest(
                    "A terminated staff member cannot be a reporting manager"
            );
        }
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

        if (pageSize < 1) {
            pageSize = 20;
        }

        return PageRequest.of(pageNumber, pageSize, pageable.getSort());
    }
}
