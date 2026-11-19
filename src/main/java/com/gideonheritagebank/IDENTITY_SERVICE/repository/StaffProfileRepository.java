package com.gideonheritagebank.IDENTITY_SERVICE.repository;

import com.gideonheritagebank.IDENTITY_SERVICE.entity.StaffProfile;
import com.gideonheritagebank.IDENTITY_SERVICE.enums.EmploymentStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface StaffProfileRepository extends JpaRepository<StaffProfile, UUID> {

    Optional<StaffProfile> findByUser_Id(UUID userId);

    Optional<StaffProfile> findByEmployeeNumber(String employeeNumber);

    @Modifying
    @Query("""
        UPDATE StaffProfile sp
        SET sp.lastLoginAt = :loginAt
        WHERE sp.user.id = :userId
        """)
    int updateLastLoginAtByUserId(
            @Param("userId") UUID userId,
            @Param("loginAt") Instant loginAt
    );

    boolean existsByEmployeeNumber(String employeeNumber);

    boolean existsByEmployeeNumberAndIdNot(
            String employeeNumber,
            UUID id
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT sp
            FROM StaffProfile sp
            WHERE sp.id = :staffProfileId
            """)
    Optional<StaffProfile> findByIdForUpdate(
            @Param("staffProfileId") UUID staffProfileId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT sp
            FROM StaffProfile sp
            WHERE sp.user.id = :userId
            """)
    Optional<StaffProfile> findByUserIdForUpdate(
            @Param("userId") UUID userId
    );

    long countByDepartment_IdAndEmploymentStatus(
            UUID departmentId,
            EmploymentStatus employmentStatus
    );

    @EntityGraph(attributePaths = {
            "user",
            "department",
            "branch",
            "branch.branchManager"
    })
    Optional<StaffProfile> findWithUserAndDepartmentById(UUID id);

    @EntityGraph(attributePaths = {
            "user",
            "department",
            "branch",
            "branch.branchManager"
    })
    Optional<StaffProfile> findWithUserAndDepartmentByUser_Id(UUID userId);

    long countByBranch_IdAndEmploymentStatus(
            UUID branchId,
            EmploymentStatus employmentStatus
    );

    @EntityGraph(attributePaths = {
            "user",
            "department",
            "branch",
            "branch.branchManager"
    })
    Page<StaffProfile> findByBranch_IdAndEmploymentStatusIn(
            UUID branchId,
            Collection<EmploymentStatus> employmentStatuses,
            Pageable pageable
    );

    long countByBranch_IdAndEmploymentStatusIn(
            UUID branchId,
            Collection<EmploymentStatus> employmentStatuses
    );
}