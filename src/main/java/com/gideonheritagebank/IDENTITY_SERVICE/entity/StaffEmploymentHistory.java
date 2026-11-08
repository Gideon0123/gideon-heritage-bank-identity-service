package com.gideonheritagebank.IDENTITY_SERVICE.entity;

import com.gideonheritagebank.IDENTITY_SERVICE.enums.EmploymentStatus;
import com.gideonheritagebank.IDENTITY_SERVICE.enums.EmploymentType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(
        name = "staff_employment_history",
        indexes = {
                @Index(
                        name = "idx_staff_employment_history_staff",
                        columnList = "staff_profile_id"
                ),
                @Index(
                        name = "idx_staff_employment_history_department",
                        columnList = "department_id"
                ),
                @Index(
                        name = "idx_staff_employment_history_branch",
                        columnList = "branch_id"
                ),
                @Index(
                        name = "idx_staff_employment_history_effective_dates",
                        columnList = "effective_from,effective_to"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StaffEmploymentHistory {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "staff_profile_id",
            nullable = false
    )
    private StaffProfile staffProfile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    /*
     * Historical snapshots.
     *
     * These preserve the department's code and name
     * even if the Department entity is renamed later.
     */
    @Column(name = "department_code_snapshot", length = 30)
    private String departmentCodeSnapshot;

    @Column(name = "department_name_snapshot", length = 120)
    private String departmentNameSnapshot;

    /*
     * Reference to the future Branch Service.
     * There is deliberately no JPA relationship.
     */
    @Column(name = "branch_id")
    private UUID branchId;

    @Column(name = "job_title", length = 120)
    private String jobTitle;

    @Enumerated(EnumType.STRING)
    @Column(name = "employment_type", length = 30)
    private EmploymentType employmentType;

    @Enumerated(EnumType.STRING)
    @Column(name = "employment_status", nullable = false, length = 30)
    private EmploymentStatus employmentStatus;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Column(name = "change_reason", length = 500)
    private String changeReason;

    @Column(name = "changed_by_user_id")
    private UUID changedByUserId;

    @CreationTimestamp
    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private Instant createdAt;

    public StaffEmploymentHistory(
            StaffProfile staffProfile,
            Department department,
            UUID branchId,
            String jobTitle,
            EmploymentType employmentType,
            EmploymentStatus employmentStatus,
            LocalDate effectiveFrom,
            String changeReason,
            UUID changedByUserId
    ) {

        this.staffProfile = staffProfile;
        this.department = department;

        this.departmentCodeSnapshot = department != null
                ? department.getDepartmentCode()
                : null;

        this.departmentNameSnapshot = department != null
                ? department.getName()
                : null;

        this.branchId = branchId;
        this.jobTitle = jobTitle;
        this.employmentType = employmentType;
        this.employmentStatus = employmentStatus;
        this.effectiveFrom = effectiveFrom;
        this.changeReason = changeReason;
        this.changedByUserId = changedByUserId;
    }

}