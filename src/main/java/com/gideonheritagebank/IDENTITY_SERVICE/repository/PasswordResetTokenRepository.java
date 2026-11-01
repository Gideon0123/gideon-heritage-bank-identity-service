package com.gideonheritagebank.IDENTITY_SERVICE.repository;

import com.gideonheritagebank.IDENTITY_SERVICE.entity.PasswordResetToken;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PasswordResetTokenRepository
        extends JpaRepository<PasswordResetToken, UUID> {

    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    List<PasswordResetToken> findAllByUser_Id(UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT token
            FROM PasswordResetToken token
            JOIN FETCH token.user
            WHERE token.tokenHash = :tokenHash
            """)
    Optional<PasswordResetToken> findByTokenHashForUpdate(
            @Param("tokenHash") String tokenHash
    );
}