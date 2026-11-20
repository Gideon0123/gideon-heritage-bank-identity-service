package com.gideonheritagebank.IDENTITY_SERVICE.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(
        name = "email_change_tokens",
        indexes = {
                @Index(
                        name = "idx_email_change_tokens_user",
                        columnList = "user_id"
                ),
                @Index(
                        name = "idx_email_change_tokens_expires",
                        columnList = "expires_at"
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EmailChangeToken {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_email_change_tokens_user"
            )
    )
    private BankUser user;

    @Column(
            name = "old_email",
            nullable = false,
            length = 254
    )
    private String oldEmail;

    @Column(
            name = "new_email",
            nullable = false,
            length = 254
    )
    private String newEmail;

    /*
     * Only the SHA-256 hash is stored.
     * The raw token is sent through the encrypted outbox payload.
     */
    @Column(
            name = "token_hash",
            nullable = false,
            unique = true,
            length = 255
    )
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "used_at")
    private Instant usedAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @CreationTimestamp
    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private Instant createdAt;

    public EmailChangeToken(
            BankUser user,
            String oldEmail,
            String newEmail,
            String tokenHash,
            Instant expiresAt
    ) {
        this.user = user;
        this.oldEmail = oldEmail;
        this.newEmail = newEmail;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
    }

    public boolean isUsed() {
        return usedAt != null;
    }

    public boolean isRevoked() {
        return revokedAt != null;
    }

    public void markUsed(Instant usedAt) {
        this.usedAt = usedAt;
    }

    public void revoke(Instant revokedAt) {
        this.revokedAt = revokedAt;
    }
}
