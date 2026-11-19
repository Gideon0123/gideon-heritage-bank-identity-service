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

    @EntityGraph(attributePaths = {
            "branchManager",
            "branchManager.staffProfile"
    })
    Optional<Branch> findWithManagerById(UUID id);

    @EntityGraph(attributePaths = {
            "branchManager",
            "branchManager.staffProfile"
    })
    Optional<Branch> findWithManagerByBranchCodeIgnoreCase(
            String branchCode
    );

    boolean existsByBranchCodeIgnoreCase(String branchCode);

    boolean existsByBranchCodeIgnoreCaseAndIdNot(
            String branchCode,
            UUID id
    );

    boolean existsByBranchManager_Id(UUID branchManagerId);

    boolean existsByBranchManager_IdAndIdNot(
            UUID branchManagerId,
            UUID branchId
    );

    @EntityGraph(attributePaths = {
            "branchManager",
            "branchManager.staffProfile"
    })
    @Query(
            value = """
                SELECT b
                FROM Branch b
                WHERE
                    (
                        COALESCE(:keyword, '') = ''
                        OR LOWER(b.branchCode) LIKE
                            CONCAT('%', LOWER(COALESCE(:keyword, '')), '%')
                        OR LOWER(b.name) LIKE
                            CONCAT('%', LOWER(COALESCE(:keyword, '')), '%')
                        OR LOWER(b.city) LIKE
                            CONCAT('%', LOWER(COALESCE(:keyword, '')), '%')
                        OR LOWER(b.stateOrRegion) LIKE
                            CONCAT('%', LOWER(COALESCE(:keyword, '')), '%')
                        OR LOWER(b.email) LIKE
                            CONCAT('%', LOWER(COALESCE(:keyword, '')), '%')
                    )
                    AND (
                        :status IS NULL
                        OR b.status = :status
                    )
                    AND (
                        COALESCE(:city, '') = ''
                        OR LOWER(b.city) = LOWER(COALESCE(:city, ''))
                    )
                """,
            countQuery = """
                SELECT COUNT(b)
                FROM Branch b
                WHERE
                    (
                        COALESCE(:keyword, '') = ''
                        OR LOWER(b.branchCode) LIKE
                            CONCAT('%', LOWER(COALESCE(:keyword, '')), '%')
                        OR LOWER(b.name) LIKE
                            CONCAT('%', LOWER(COALESCE(:keyword, '')), '%')
                        OR LOWER(b.city) LIKE
                            CONCAT('%', LOWER(COALESCE(:keyword, '')), '%')
                        OR LOWER(b.stateOrRegion) LIKE
                            CONCAT('%', LOWER(COALESCE(:keyword, '')), '%')
                        OR LOWER(b.email) LIKE
                            CONCAT('%', LOWER(COALESCE(:keyword, '')), '%')
                    )
                    AND (
                        :status IS NULL
                        OR b.status = :status
                    )
                    AND (
                        COALESCE(:city, '') = ''
                        OR LOWER(b.city) = LOWER(COALESCE(:city, ''))
                    )
                """
    )
    Page<Branch> searchBranches(
            @Param("keyword") String keyword,
            @Param("status") BranchStatus status,
            @Param("city") String city,
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

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT b
        FROM Branch b
        WHERE b.branchManager.id = :userId
        """)
    Optional<Branch> findByBranchManagerIdForUpdate(
            @Param("userId") UUID userId
    );
}

