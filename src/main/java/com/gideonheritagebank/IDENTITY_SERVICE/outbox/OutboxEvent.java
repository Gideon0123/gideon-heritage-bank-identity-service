package com.gideonheritagebank.IDENTITY_SERVICE.outbox;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "identity_outbox_events",
        indexes = {
                @Index(
                        name = "idx_identity_outbox_pending",
                        columnList = "status, available_at, created_at"
                ),
                @Index(
                        name = "idx_identity_outbox_processing",
                        columnList = "status, locked_until"
                )
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_identity_outbox_event_message_id",
                        columnNames = {"event_type", "message_id"}
                )
        }
)
@Getter
@Setter
public class OutboxEvent {

    @Id
    private UUID id;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(name = "exchange_name", nullable = false, length = 255)
    private String exchangeName;

    @Column(name = "routing_key", nullable = false, length = 255)
    private String routingKey;

    @Column(name = "message_id", nullable = false, length = 255)
    private String messageId;

    /*
     * This is encrypted JSON, not plaintext.
     * The original payload contains one-time credential tokens.
     */
    @Column(
            name = "encrypted_payload",
            nullable = false,
            columnDefinition = "TEXT"
    )
    private String encryptedPayload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OutboxStatus status;

    @Column(nullable = false)
    private int attempts;

    @Column(name = "available_at", nullable = false)
    private Instant availableAt;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Column(name = "lock_owner", length = 100)
    private String lockOwner;

    @Column(name = "last_error", length = 500)
    private String lastError;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    /*
     * Protects against stale relay workers overwriting newer state.
     */
    @Version
    @Column(nullable = false)
    private long version;

    protected OutboxEvent() {
    }

    public static OutboxEvent pending(
            String eventType,
            String exchangeName,
            String routingKey,
            String messageId,
            String encryptedPayload,
            Instant now
    ) {
        OutboxEvent event = new OutboxEvent();

        event.id = UUID.randomUUID();
        event.eventType = eventType;
        event.exchangeName = exchangeName;
        event.routingKey = routingKey;
        event.messageId = messageId;
        event.encryptedPayload = encryptedPayload;
        event.status = OutboxStatus.PENDING;
        event.attempts = 0;
        event.availableAt = now;
        event.createdAt = now;
        event.updatedAt = now;

        return event;
    }

    public void claim(
            String owner,
            Instant leaseUntil,
            Instant now
    ) {
        this.status = OutboxStatus.PROCESSING;
        this.attempts++;
        this.lockOwner = owner;
        this.lockedUntil = leaseUntil;
        this.updatedAt = now;
        this.lastError = null;
    }

    public void markPublished(
            String owner,
            Instant now
    ) {
        requireOwner(owner);

        this.status = OutboxStatus.PUBLISHED;
        this.publishedAt = now;
        this.updatedAt = now;

        clearLease();
        this.lastError = null;
    }

    public void scheduleRetry(
            String owner,
            Instant nextAttemptAt,
            String errorCode,
            Instant now
    ) {
        requireOwner(owner);

        this.status = OutboxStatus.PENDING;
        this.availableAt = nextAttemptAt;
        this.lastError = truncate(errorCode);
        this.updatedAt = now;

        clearLease();
    }

    public void markFailed(
            String owner,
            String errorCode,
            Instant now
    ) {
        requireOwner(owner);

        this.status = OutboxStatus.FAILED;
        this.lastError = truncate(errorCode);
        this.updatedAt = now;

        clearLease();
    }

    private void requireOwner(String owner) {
        if (this.status != OutboxStatus.PROCESSING
                || this.lockOwner == null
                || !this.lockOwner.equals(owner)) {

            throw new IllegalStateException(
                    "Outbox event is no longer owned by this relay worker"
            );
        }
    }

    private void clearLease() {
        this.lockOwner = null;
        this.lockedUntil = null;
    }

    private String truncate(String value) {
        if (value == null) {
            return null;
        }

        return value.length() <= 500 ? value : value.substring(0, 500);
    }

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();

        if (createdAt == null) {
            createdAt = now;
        }

        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

}