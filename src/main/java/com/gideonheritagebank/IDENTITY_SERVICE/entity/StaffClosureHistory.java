package com.gideonheritagebank.IDENTITY_SERVICE.entity;


import com.gideonheritagebank.IDENTITY_SERVICE.enums.StaffRole;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(
        name = "staff_closure_history",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_staff_closure_history_profile",
                        columnNames = "staff_profile_id"
                ),
                @UniqueConstraint(
                        name = "uq_staff_closure_history_user",
                        columnNames = "user_id"
                )
        },
        indexes = {
                @Index(
                        name = "idx_staff_closure_history_closed_at",
                        columnList = "closed_at"
                ),
                @Index(
                        name = "idx_staff_closure_history_admin",
                        columnList = "closed_by_user_id"
                ),
                @Index(
                        name = "idx_staff_closure_history_retention_review",
                        columnList = "retention_review_at"
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StaffClosureHistory {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "staff_profile_id",
            nullable = false,
            unique = true
    )
    private StaffProfile staffProfile;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "employee_number", nullable = false, length = 50)
    private String employeeNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "final_staff_role", length = 50)
    private StaffRole finalStaffRole;

    @Column(name = "closure_reason", nullable = false, length = 1000)
    private String closureReason;

    @Column(name = "closed_by_user_id", nullable = false)
    private UUID closedByUserId;

    @Column(name = "closed_at", nullable = false)
    private Instant closedAt;

    @Column(name = "retention_review_at", nullable = false)
    private Instant retentionReviewAt;

    public StaffClosureHistory(
            StaffProfile staffProfile,
            UUID userId,
            String employeeNumber,
            StaffRole finalStaffRole,
            String closureReason,
            UUID closedByUserId,
            Instant closedAt,
            Instant retentionReviewAt
    ) {

        this.staffProfile = Objects.requireNonNull(staffProfile);
        this.userId = Objects.requireNonNull(userId);
        this.employeeNumber = Objects.requireNonNull(employeeNumber);
        this.finalStaffRole = finalStaffRole;
        this.closureReason = Objects.requireNonNull(closureReason);
        this.closedByUserId = Objects.requireNonNull(closedByUserId);
        this.closedAt = Objects.requireNonNull(closedAt);
        this.retentionReviewAt = Objects.requireNonNull(retentionReviewAt);
    }
}

