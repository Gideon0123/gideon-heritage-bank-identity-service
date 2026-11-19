package com.gideonheritagebank.IDENTITY_SERVICE.entity;

import com.gideonheritagebank.IDENTITY_SERVICE.enums.BranchStatus;
import com.gideonheritagebank.IDENTITY_SERVICE.enums.BranchType;
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

@Entity
@Table(
        name = "branches",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_branches_branch_code",
                        columnNames = "branch_code"
                ),
                @UniqueConstraint(
                        name = "uk_branches_branch_manager",
                        columnNames = "branch_manager_id"
                )
        },
        indexes = {
                @Index(
                        name = "idx_branches_status",
                        columnList = "status"
                ),
                @Index(
                        name = "idx_branches_city",
                        columnList = "city"
                ),
                @Index(
                        name = "idx_branches_state",
                        columnList = "state_or_region"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Branch {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(
            name = "branch_code",
            nullable = false,
            length = 50,
            updatable = false
    )
    private String branchCode;

    @Column(
            name = "name",
            nullable = false,
            length = 120
    )
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "branch_type",
            nullable = false,
            length = 30
    )
    private BranchType branchType;

    // PHYSICAL ADDRESS

    @Column(
            name = "address_line1",
            nullable = false,
            length = 180
    )
    private String addressLine1;

    @Column(
            name = "address_line2",
            length = 180
    )
    private String addressLine2;

    @Column(
            name = "city",
            nullable = false,
            length = 100
    )
    private String city;

    @Column(
            name = "state_or_region",
            nullable = false,
            length = 100
    )
    private String stateOrRegion;

    @Column(
            name = "postal_code",
            length = 20
    )
    private String postalCode;

    @Column(
            name = "country_code",
            length = 2,
            nullable = false
    )
    private String countryCode = "NG";

    // CONTACT INFORMATION
    @Column(
            name = "phone_number",
            length = 16
    )
    private String phoneNumber;

    @Column(
            name = "email",
            length = 254
    )
    private String email;

    // BRANCH MANAGER
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "branch_manager_id",
            foreignKey = @ForeignKey(
                    name = "fk_branches_branch_manager"
            )
    )
    private BankUser branchManager;

    // BRANCH STATUS
    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            nullable = false,
            length = 30
    )
    private BranchStatus status = BranchStatus.ACTIVE;

    @Column(
            name = "opened_date",
            nullable = false
    )
    private LocalDate openedDate;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(
            name = "closure_reason",
            length = 1000
    )
    private String closureReason;

    // AUDIT TIMESTAMPS

    @CreationTimestamp
    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private Instant createdAt;

    @UpdateTimestamp
    @Column(
            name = "updated_at",
            nullable = false
    )
    private Instant updatedAt;

    // CONSTRUCTOR
    public Branch(
            String branchCode,
            String name,
            BranchType branchType,
            String addressLine1,
            String addressLine2,
            String city,
            String stateOrRegion,
            String postalCode,
            String countryCode,
            String phoneNumber,
            String email,
            LocalDate openedDate
    ) {
        this.branchCode = branchCode;
        this.name = name;
        this.branchType = branchType;
        this.addressLine1 = addressLine1;
        this.addressLine2 = addressLine2;
        this.city = city;
        this.stateOrRegion = stateOrRegion;
        this.postalCode = postalCode;
        this.countryCode = countryCode;
        this.phoneNumber = phoneNumber;
        this.email = email;
        this.openedDate = openedDate;
        this.status = BranchStatus.ACTIVE;
    }

    // FACTORY METHOD

    public static Branch create(
            String branchCode,
            String name,
            BranchType branchType,
            String addressLine1,
            String addressLine2,
            String city,
            String stateOrRegion,
            String postalCode,
            String countryCode,
            String phoneNumber,
            String email,
            LocalDate openedDate
    ) {
        return new Branch(
                branchCode,
                name,
                branchType,
                addressLine1,
                addressLine2,
                city,
                stateOrRegion,
                postalCode,
                countryCode,
                phoneNumber,
                email,
                openedDate
        );
    }

    // BRANCH MANAGER

    public void assignBranchManager(BankUser manager) {
        this.branchManager = manager;
    }

    public void removeBranchManager() {
        this.branchManager = null;
    }

    // BRANCH LIFECYCLE

    public void activate() {
        if (this.status == BranchStatus.CLOSED) {
            throw new IllegalStateException(
                    "A permanently closed branch cannot be reactivated"
            );
        }

        this.status = BranchStatus.ACTIVE;
    }

    public void deactivate() {
        if (this.status == BranchStatus.CLOSED) {
            throw new IllegalStateException(
                    "A permanently closed branch cannot be deactivated"
            );
        }

        this.status = BranchStatus.INACTIVE;
    }

    public void close(
            Instant closedAt,
            String closureReason
    ) {
        if (this.status == BranchStatus.CLOSED) {
            throw new IllegalStateException(
                    "This branch is already closed"
            );
        }

        this.status = BranchStatus.CLOSED;
        this.closedAt = closedAt;
        this.closureReason = closureReason;
    }

}
