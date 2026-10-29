package com.gideonheritagebank.IDENTITY_SERVICE.entity;

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
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StaffProfile {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private BankUser user;

    @Column(name = "employee_number", nullable = false, unique = true, length = 50)
    private String employeeNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "staff_role", nullable = false, length = 50)
    private StaffRole staffRole;

    @Column(name = "hire_date", nullable = false)
    private LocalDate hireDate;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public StaffProfile(
            String employeeNumber,
            StaffRole staffRole,
            LocalDate hireDate
    ) {
        this.employeeNumber = employeeNumber;
        this.staffRole = staffRole;
        this.hireDate = hireDate;
    }

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

        return profile;
    }

}