package com.gideonheritagebank.IDENTITY_SERVICE.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "departments",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_departments_code",
                        columnNames = "department_code"
                )
        },
        indexes = {
                @Index(
                        name = "idx_departments_active",
                        columnList = "is_active"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Department {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(
            name = "department_code",
            nullable = false,
            length = 30
    )
    private String departmentCode;

    @Column(
            name = "name",
            nullable = false,
            length = 120
    )
    private String name;

    @Column(length = 500)
    private String description;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

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

    public Department(
            String departmentCode,
            String name,
            String description
    ) {
        this.departmentCode = departmentCode;
        this.name = name;
        this.description = description;
        this.active = true;
    }

}