package com.gideonheritagebank.IDENTITY_SERVICE.repository;

import com.gideonheritagebank.IDENTITY_SERVICE.entity.StaffEmploymentHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface StaffEmploymentHistoryRepository extends JpaRepository<StaffEmploymentHistory, UUID> {

    @EntityGraph(attributePaths = {"department"})
    Page<StaffEmploymentHistory> findAllByStaffProfile_IdOrderByEffectiveFromDescCreatedAtDesc(
            UUID staffProfileId,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"department"})
    Optional<StaffEmploymentHistory> findByIdAndStaffProfile_Id(
            UUID historyId,
            UUID staffProfileId
    );

    @EntityGraph(attributePaths = {"department"})
    Optional<StaffEmploymentHistory> findFirstByStaffProfile_IdAndEffectiveToIsNull(
            UUID staffProfileId
    );

    boolean existsByStaffProfile_IdAndEffectiveToIsNull(
            UUID staffProfileId
    );
}
