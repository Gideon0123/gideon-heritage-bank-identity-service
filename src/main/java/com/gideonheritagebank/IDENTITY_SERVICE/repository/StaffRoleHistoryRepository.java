package com.gideonheritagebank.IDENTITY_SERVICE.repository;

import com.gideonheritagebank.IDENTITY_SERVICE.entity.StaffRoleHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface StaffRoleHistoryRepository extends JpaRepository<StaffRoleHistory, UUID> {

    Page<StaffRoleHistory> findAllByStaffProfile_IdOrderByChangedAtDesc(
            UUID staffProfileId,
            Pageable pageable
    );

    Page<StaffRoleHistory> findAllByStaffProfile_Id(
            UUID staffProfileId,
            Pageable pageable
    );
}
