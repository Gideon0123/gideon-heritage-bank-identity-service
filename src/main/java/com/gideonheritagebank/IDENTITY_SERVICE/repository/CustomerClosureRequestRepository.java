package com.gideonheritagebank.IDENTITY_SERVICE.repository;

import com.gideonheritagebank.IDENTITY_SERVICE.entity.CustomerClosureRequest;
import com.gideonheritagebank.IDENTITY_SERVICE.enums.CustomerClosureStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CustomerClosureRequestRepository
        extends JpaRepository<CustomerClosureRequest, UUID> {

    List<CustomerClosureRequest>
    findAllByCustomer_IdOrderByRequestedAtDesc(UUID customerId);

    Optional<CustomerClosureRequest>
    findByIdAndCustomer_Id(UUID requestId, UUID customerId);

    boolean existsByCustomer_IdAndStatusIn(
            UUID customerId,
            Collection<CustomerClosureStatus> statuses
    );

    Page<CustomerClosureRequest> findAllByStatusIn(
            Collection<CustomerClosureStatus> statuses,
            Pageable pageable
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT request
            FROM CustomerClosureRequest request
            JOIN FETCH request.customer
            WHERE request.id = :requestId
            """)
    Optional<CustomerClosureRequest> findByIdForUpdate(
            @Param("requestId") UUID requestId
    );
}