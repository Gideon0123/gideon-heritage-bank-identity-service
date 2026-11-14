package com.gideonheritagebank.IDENTITY_SERVICE.repository;

import com.gideonheritagebank.IDENTITY_SERVICE.entity.StaffOnboardingToken;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StaffOnboardingTokenRepository extends JpaRepository<StaffOnboardingToken, UUID> {

    Optional<StaffOnboardingToken> findByTokenHash(String tokenHash);

    List<StaffOnboardingToken> findAllByUser_Id(UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT token
            FROM StaffOnboardingToken token
            JOIN FETCH token.user
            WHERE token.tokenHash = :tokenHash
            """)
    Optional<StaffOnboardingToken> findByTokenHashForUpdate(
            @Param("tokenHash") String tokenHash
    );

    @org.springframework.data.jpa.repository.Modifying
    @Query("""
            DELETE FROM StaffOnboardingToken token
            WHERE token.expiresAt < :cutoff
               OR token.usedAt < :cutoff
               OR token.revokedAt < :cutoff
            """)
    int deleteOldTokenRecords(
            @Param("cutoff") Instant cutoff
    );
}
