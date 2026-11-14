package com.gideonheritagebank.IDENTITY_SERVICE.repository;


import com.gideonheritagebank.IDENTITY_SERVICE.entity.StaffClosureHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface StaffClosureHistoryRepository
        extends JpaRepository<StaffClosureHistory, UUID> {

    Optional<StaffClosureHistory> findByUserId(UUID userId);

    Optional<StaffClosureHistory> findByStaffProfile_Id(UUID staffProfileId);

    boolean existsByUserId(UUID userId);

    Page<StaffClosureHistory> findAllByOrderByClosedAtDesc(
            Pageable pageable
    );

    Page<StaffClosureHistory> findAllByClosedByUserId(
            UUID adminUserId,
            Pageable pageable
    );
}
