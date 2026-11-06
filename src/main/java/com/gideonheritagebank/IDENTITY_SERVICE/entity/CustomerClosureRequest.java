package com.gideonheritagebank.IDENTITY_SERVICE.entity;

import com.gideonheritagebank.IDENTITY_SERVICE.enums.CustomerClosureStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "customer_closure_requests",
        indexes = {
                @Index(
                        name = "idx_closure_requests_customer",
                        columnList = "customer_id"
                ),
                @Index(
                        name = "idx_closure_requests_status",
                        columnList = "status"
                ),
                @Index(
                        name = "idx_closure_requests_requested_at",
                        columnList = "requested_at"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CustomerClosureRequest {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private BankUser customer;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    private CustomerClosureStatus status;

    @Column(name = "customer_reason", nullable = false, length = 1000)
    private String customerReason;

    @CreationTimestamp
    @Column(name = "requested_at", nullable = false, updatable = false)
    private Instant requestedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private BankUser reviewedBy;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "review_notes", length = 1000)
    private String reviewNotes;

    @Column(name = "financial_clearance_reference", length = 150)
    private String financialClearanceReference;

    @Column(name = "financial_clearance_notes", length = 1000)
    private String financialClearanceNotes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clearance_confirmed_by")
    private BankUser clearanceConfirmedBy;

    @Column(name = "financial_clearance_confirmed_at")
    private Instant financialClearanceConfirmedAt;

    @Column(name = "rejection_reason", length = 1000)
    private String rejectionReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rejected_by")
    private BankUser rejectedBy;

    @Column(name = "rejected_at")
    private Instant rejectedAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "completed_by")
    private BankUser completedBy;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Version
    private long version;

    public CustomerClosureRequest(
            BankUser customer,
            String customerReason
    ) {
        this.customer = customer;
        this.customerReason = customerReason;
        this.status = CustomerClosureStatus.REQUESTED;
    }

    public void beginReview(
            BankUser reviewer,
            String notes
    ) {
        requireStatus(CustomerClosureStatus.REQUESTED);

        this.status = CustomerClosureStatus.UNDER_REVIEW;
        this.reviewedBy = reviewer;
        this.reviewedAt = Instant.now();
        this.reviewNotes = notes;
    }

    public void requestFinancialClearance(
            String notes
    ) {
        requireStatus(CustomerClosureStatus.UNDER_REVIEW);

        this.status = CustomerClosureStatus.AWAITING_FINANCIAL_CLEARANCE;
        this.financialClearanceNotes = notes;
    }

    public void confirmFinancialClearance(
            BankUser confirmer,
            String reference,
            String notes
    ) {
        requireStatus(CustomerClosureStatus.AWAITING_FINANCIAL_CLEARANCE);

        this.status = CustomerClosureStatus.READY_TO_CLOSE;
        this.clearanceConfirmedBy = confirmer;
        this.financialClearanceReference = reference;
        this.financialClearanceNotes = notes;
        this.financialClearanceConfirmedAt = Instant.now();
    }

    public void reject(
            BankUser reviewer,
            String reason
    ) {
        if (this.status == CustomerClosureStatus.COMPLETED
                || this.status == CustomerClosureStatus.REJECTED
                || this.status == CustomerClosureStatus.CANCELLED) {
            throw new IllegalStateException(
                    "This closure request can no longer be rejected."
            );
        }

        this.status = CustomerClosureStatus.REJECTED;
        this.rejectedBy = reviewer;
        this.rejectedAt = Instant.now();
        this.rejectionReason = reason;
    }

    public void cancel() {
        requireStatus(CustomerClosureStatus.REQUESTED);

        this.status = CustomerClosureStatus.CANCELLED;
        this.cancelledAt = Instant.now();
    }

    public void complete(BankUser reviewer) {
        requireStatus(CustomerClosureStatus.READY_TO_CLOSE);

        this.status = CustomerClosureStatus.COMPLETED;
        this.completedBy = reviewer;
        this.completedAt = Instant.now();
    }

    private void requireStatus(CustomerClosureStatus expected) {
        if (this.status != expected) {
            throw new com.gideonheritagebank.IDENTITY_SERVICE.exception.IllegalStateException(
                    "Invalid closure request transition. Expected "
                            + expected + " but current status is " + this.status
            );
        }
    }
}
