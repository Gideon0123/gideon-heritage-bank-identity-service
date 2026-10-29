package com.gideonheritagebank.IDENTITY_SERVICE.repository;

import com.gideonheritagebank.IDENTITY_SERVICE.entity.BankUser;
import org.springframework.data.jpa.repository.JpaRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface BankUserRepository extends JpaRepository<BankUser, UUID> {

    Optional<BankUser> findByEmail(String email);

    Optional<BankUser> findByPhoneNumber(String phoneNumber);

    boolean existsByEmail(String email);

    boolean existsByPhoneNumber(String phoneNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM BankUser u WHERE u.id = :userId")
    Optional<BankUser> findByIdForUpdate(
            @Param("userId") UUID userId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
       SELECT u
       FROM BankUser u
       WHERE LOWER(u.email) = LOWER(:email)
       """)
    Optional<BankUser> findByEmailForUpdate(
            @Param("email") String email
    );
}