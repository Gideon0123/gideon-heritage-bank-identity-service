package com.gideonheritagebank.IDENTITY_SERVICE.repository;

import com.gideonheritagebank.IDENTITY_SERVICE.outbox.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface OutboxEventRepository
        extends JpaRepository<OutboxEvent, UUID> {

    /*
     * PostgreSQL row locking:
     *
     * - PENDING rows whose retry time has arrived are eligible.
     * - PROCESSING rows whose lease expired are eligible for recovery.
     * - SKIP LOCKED allows multiple service instances to claim
     *   different rows without blocking one another.
     *
     * This query must execute inside a transaction.
     */
    @Query(value = """
            SELECT *
            FROM identity_outbox_events
            WHERE (status = 'PENDING' AND available_at <= :now)
               OR (status = 'PROCESSING' AND locked_until <= :now)
            ORDER BY created_at ASC
            LIMIT :batchSize
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<OutboxEvent> findBatchForRelay(
            @Param("now") Instant now,
            @Param("batchSize") int batchSize
    );
}
