package com.gideonheritagebank.IDENTITY_SERVICE.repository;

import com.gideonheritagebank.IDENTITY_SERVICE.entity.CustomerStaffConversion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CustomerStaffConversionRepository
        extends JpaRepository<CustomerStaffConversion, UUID> {

    Optional<CustomerStaffConversion> findByUser_Id(UUID userId);

    Page<CustomerStaffConversion> findAllByUser_Id(
            UUID userId,
            Pageable pageable
    );

    boolean existsByUser_Id(UUID userId);
}