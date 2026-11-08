package com.gideonheritagebank.IDENTITY_SERVICE.repository;

import com.gideonheritagebank.IDENTITY_SERVICE.entity.StaffProfile;
import com.gideonheritagebank.IDENTITY_SERVICE.enums.EmploymentStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface StaffProfileRepository
        extends JpaRepository<StaffProfile, UUID> {

    Optional<StaffProfile> findByUser_Id(UUID userId);

    Optional<StaffProfile> findByEmployeeNumber(String employeeNumber);

    boolean existsByEmployeeNumber(String employeeNumber);

    boolean existsByEmployeeNumberAndIdNot(
            String employeeNumber,
            UUID id
    );

    @EntityGraph(attributePaths = {"user", "department"})
    Optional<StaffProfile> findWithUserAndDepartmentById(UUID id);

    @EntityGraph(attributePaths = {"user", "department"})
    Optional<StaffProfile> findWithUserAndDepartmentByUser_Id(UUID userId);

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
}