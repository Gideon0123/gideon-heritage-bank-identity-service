package com.gideonheritagebank.IDENTITY_SERVICE.entity;

import com.gideonheritagebank.IDENTITY_SERVICE.enums.EmploymentStatus;
import com.gideonheritagebank.IDENTITY_SERVICE.enums.EmploymentType;
import com.gideonheritagebank.IDENTITY_SERVICE.enums.StaffRole;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(
        name = "staff_profiles",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_staff_profiles_user_id",
                        columnNames = "user_id"
                ),
                @UniqueConstraint(
                        name = "uk_staff_profiles_employee_number",
                        columnNames = "employee_number"
                )
        },
        indexes = {
                @Index(
                        name = "idx_staff_profiles_department",
                        columnList = "department_id"
                ),
                @Index(
                        name = "idx_staff_profiles_branch",
                        columnList = "branch_id"
                ),
                @Index(
                        name = "idx_staff_profiles_employment_status",
                        columnList = "employment_status"
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StaffProfile {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            unique = true
    )
    private BankUser user;

    @Column(
            name = "employee_number",
            nullable = false,
            unique = true,
            length = 50
    )
    private String employeeNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "staff_role", length = 50)
    private StaffRole staffRole;

    /*
     * Original employment hire date.
     * This does not change during transfers or promotions.
     */
    @Column(name = "hire_date", nullable = false)
    private LocalDate hireDate;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    // CURRENT EMPLOYMENT ASSIGNMENT
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    /* Reference to the future Branch Service. */
    @Column(name = "branch_id")
    private UUID branchId;

    @Column(name = "job_title", length = 120)
    private String jobTitle;

    @Enumerated(EnumType.STRING)
    @Column(name = "employment_type", length = 30)
    private EmploymentType employmentType;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "employment_status",
            nullable = false,
            length = 30
    )
    private EmploymentStatus employmentStatus = EmploymentStatus.PENDING;

    /*
     * Beginning of the current assignment.
     * This is different from the original hire date.
     */
    @Column(name = "assignment_start_date", nullable = false)
    private LocalDate assignmentStartDate;

    /* Populated when employment is terminated. */
    @Column(name = "employment_end_date")
    private LocalDate employmentEndDate;

    /*
     * Reference to the reporting manager's BankUser ID.
     * This avoids a second JPA relationship to BankUser.
     */
    @Column(name = "reporting_manager_id")
    private UUID reportingManagerId;

    // EMPLOYMENT HISTORY
    @OneToMany(
            mappedBy = "staffProfile",
            cascade = {
                    CascadeType.PERSIST,
                    CascadeType.MERGE
            }
    )
    private List<StaffEmploymentHistory> employmentHistory = new ArrayList<>();

    // AUDIT TIMESTAMPS
    @CreationTimestamp
    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // CONSTRUCTORS
    public StaffProfile(
            String employeeNumber,
            StaffRole staffRole,
            LocalDate hireDate
    ) {
        this.employeeNumber = employeeNumber;
        this.staffRole = staffRole;
        this.hireDate = hireDate;
        this.assignmentStartDate = hireDate;
        this.employmentStatus = EmploymentStatus.PENDING;
    }

    // FACTORY METHOD
    public static StaffProfile create(
            BankUser user,
            String employeeNumber,
            StaffRole staffRole,
            LocalDate hireDate
    ) {
        StaffProfile profile = new StaffProfile();

        profile.setUser(user);
        profile.setEmployeeNumber(employeeNumber);
        profile.setStaffRole(staffRole);
        profile.setHireDate(hireDate);
        profile.setAssignmentStartDate(hireDate);
        profile.setEmploymentStatus(EmploymentStatus.PENDING);

        return profile;
    }

    // EMPLOYMENT HISTORY HELPERS
    public void addEmploymentHistory(
            StaffEmploymentHistory history
    ) {
        if (history == null) {
            throw new IllegalArgumentException(
                    "Employment history cannot be null"
            );
        }

        employmentHistory.add(history);
    }

    public void removeEmploymentHistory(
            StaffEmploymentHistory history
    ) {
        employmentHistory.remove(history);
    }

}