package com.gideonheritagebank.IDENTITY_SERVICE.repository;

import com.gideonheritagebank.IDENTITY_SERVICE.entity.Branch;
import com.gideonheritagebank.IDENTITY_SERVICE.enums.BranchStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface BranchRepository extends JpaRepository<Branch, UUID> {

    Optional<Branch> findByBranchCodeIgnoreCase(String branchCode);

    boolean existsByBranchCodeIgnoreCase(String branchCode);

    boolean existsByBranchCodeIgnoreCaseAndIdNot(
            String branchCode,
            UUID id
    );

    boolean existsByBranchManager_Id(UUID branchManagerId);

    @EntityGraph(attributePaths = {
            "branchManager",
            "branchManager.staffProfile"
    })
    Optional<Branch> findWithManagerById(UUID id);

    @EntityGraph(attributePaths = {
            "branchManager",
            "branchManager.staffProfile"
    })
    Page<Branch> findAllBy(Pageable pageable);

    @EntityGraph(attributePaths = {
            "branchManager",
            "branchManager.staffProfile"
    })
    Page<Branch> findAllByStatus(
            BranchStatus status,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {
            "branchManager",
            "branchManager.staffProfile"
    })
    Page<Branch> findAllByCityIgnoreCase(
            String city,
            Pageable pageable
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT b
            FROM Branch b
            WHERE b.id = :branchId
            """)
    Optional<Branch> findByIdForUpdate(
            @Param("branchId") UUID branchId
    );

    long countByStatus(BranchStatus status);

}
