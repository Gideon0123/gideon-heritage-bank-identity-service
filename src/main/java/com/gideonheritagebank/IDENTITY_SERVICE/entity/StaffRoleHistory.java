package com.gideonheritagebank.IDENTITY_SERVICE.entity;

import com.gideonheritagebank.IDENTITY_SERVICE.enums.StaffRole;
import com.gideonheritagebank.IDENTITY_SERVICE.enums.StaffRoleChangeType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "staff_role_history",
        indexes = {
                @Index(
                        name = "idx_staff_role_history_profile",
                        columnList = "staff_profile_id"
                ),
                @Index(
                        name = "idx_staff_role_history_changed_at",
                        columnList = "changed_at"
                ),
                @Index(
                        name = "idx_staff_role_history_changed_by",
                        columnList = "changed_by_user_id"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
public class StaffRoleHistory {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "staff_profile_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_staff_role_history_staff")
    )
    private StaffProfile staffProfile;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_role", length = 50)
    private StaffRole previousRole;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_role", length = 50)
    private StaffRole newRole;

    @Enumerated(EnumType.STRING)
    @Column(name = "change_type", nullable = false, length = 30)
    private StaffRoleChangeType changeType;

    @Column(name = "change_reason", nullable = false, length = 1000)
    private String changeReason;

    @Column(name = "changed_by_user_id", nullable = false)
    private UUID changedByUserId;

    @Column(name = "changed_at", nullable = false, updatable = false)
    private Instant changedAt;

    public StaffRoleHistory(
            StaffProfile staffProfile,
            StaffRole previousRole,
            StaffRole newRole,
            StaffRoleChangeType changeType,
            String changeReason,
            UUID changedByUserId
    ) {
        this.staffProfile = staffProfile;
        this.previousRole = previousRole;
        this.newRole = newRole;
        this.changeType = changeType;
        this.changeReason = changeReason;
        this.changedByUserId = changedByUserId;
    }

    @PrePersist
    protected void onCreate() {
        if (changedAt == null) {
            changedAt = Instant.now();
        }
    }
}
