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
        name = "customer_staff_conversions",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_customer_staff_conversions_user",
                        columnNames = "user_id"
                ),
                @UniqueConstraint(
                        name = "uk_customer_staff_conversions_employee_number",
                        columnNames = "employee_number"
                )
        },
        indexes = {
                @Index(
                        name = "idx_customer_staff_conversions_converted_at",
                        columnList = "converted_at"
                ),
                @Index(
                        name = "idx_customer_staff_conversions_admin",
                        columnList = "converted_by_user_id"
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CustomerStaffConversion {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_customer_staff_conversions_user"
            )
    )
    private BankUser user;

    @Column(
            name = "customer_number",
            nullable = false,
            length = 12,
            updatable = false
    )
    private String customerNumber;

    @Column(
            name = "employee_number",
            nullable = false,
            length = 50,
            updatable = false
    )
    private String employeeNumber;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "assigned_staff_role",
            nullable = false,
            length = 50,
            updatable = false
    )
    private StaffRole assignedStaffRole;

    @Column(
            name = "conversion_reason",
            nullable = false,
            length = 1000
    )
    private String conversionReason;

    @Column(
            name = "converted_by_user_id",
            nullable = false,
            updatable = false
    )
    private UUID convertedByUserId;

    @Column(
            name = "converted_at",
            nullable = false,
            updatable = false
    )
    private Instant convertedAt;

    public CustomerStaffConversion(
            BankUser user,
            String customerNumber,
            String employeeNumber,
            StaffRole assignedStaffRole,
            String conversionReason,
            UUID convertedByUserId
    ) {
        this.user = Objects.requireNonNull(user);
        this.customerNumber = Objects.requireNonNull(customerNumber);
        this.employeeNumber = Objects.requireNonNull(employeeNumber);
        this.assignedStaffRole = Objects.requireNonNull(assignedStaffRole);
        this.conversionReason = Objects.requireNonNull(conversionReason);
        this.convertedByUserId = Objects.requireNonNull(convertedByUserId);
        this.convertedAt = Instant.now();
    }
}