package com.gideonheritagebank.IDENTITY_SERVICE.repository;

import com.gideonheritagebank.IDENTITY_SERVICE.entity.UserAddress;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserAddressRepository extends JpaRepository<UserAddress, UUID> {

    List<UserAddress> findAllByUser_IdOrderByCreatedAtDesc(UUID userId);

    Optional<UserAddress> findByIdAndUser_Id(UUID addressId, UUID userId);

    boolean existsByUser_Id(UUID userId);

    Page<UserAddress> findAllByUser_IdOrderByCreatedAtDesc(
            UUID userId,
            Pageable pageable
    );

}