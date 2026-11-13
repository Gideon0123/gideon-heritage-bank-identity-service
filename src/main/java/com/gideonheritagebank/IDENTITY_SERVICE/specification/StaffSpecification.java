package com.gideonheritagebank.IDENTITY_SERVICE.specification;

import com.gideonheritagebank.IDENTITY_SERVICE.entity.BankUser;
import com.gideonheritagebank.IDENTITY_SERVICE.entity.StaffProfile;
import com.gideonheritagebank.IDENTITY_SERVICE.enums.StaffIdentityStatus;
import com.gideonheritagebank.IDENTITY_SERVICE.enums.UserType;
import com.gideonheritagebank.IDENTITY_SERVICE.dto.request.StaffFilterRequest;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;

import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public final class StaffSpecification {

    private StaffSpecification() {
    }

    public static Specification<BankUser> withFilters(
            StaffFilterRequest filter
    ) {

        return (root, query, cb) -> {

            List<Predicate> predicates = new ArrayList<>();

            /*
             * Mandatory restriction:
             * staff queries must never return customer identities.
             */
            predicates.add(
                    cb.equal(root.get("userType"), UserType.STAFF)
            );

            /*
             * StaffProfile is a one-to-one association.
             * An inner join ensures that only properly provisioned
             * staff identities with a profile are returned.
             */
            Join<BankUser, StaffProfile> staffProfile =
                    root.join("staffProfile", JoinType.INNER);

            if (filter.getEnabled() != null) {
                predicates.add(
                        cb.equal(root.get("enabled"), filter.getEnabled())
                );
            }

            if (filter.getEmailVerified() != null) {
                predicates.add(
                        cb.equal(
                                root.get("emailVerified"), filter.getEmailVerified()
                        )
                );
            }

            if (filter.getAccountNonLocked() != null) {
                predicates.add(
                        cb.equal(
                                root.get("accountNonLocked"),
                                filter.getAccountNonLocked()
                        )
                );
            }

            if (filter.getSuspended() != null) {
                predicates.add(
                        cb.equal(
                                root.get("suspended"),
                                filter.getSuspended()
                        )
                );
            }

            if (filter.getStaffRole() != null) {
                predicates.add(
                        cb.equal(
                                staffProfile.get("staffRole"), filter.getStaffRole()
                        )
                );
            }

            if (filter.getCreatedFrom() != null) {
                predicates.add(
                        cb.greaterThanOrEqualTo(
                                root.get("createdAt"),
                                filter.getCreatedFrom()
                        )
                );
            }

            if (filter.getCreatedTo() != null) {
                predicates.add(
                        cb.lessThanOrEqualTo(
                                root.get("createdAt"),
                                filter.getCreatedTo()
                        )
                );
            }

            if (filter.getHireDateFrom() != null) {
                predicates.add(
                        cb.greaterThanOrEqualTo(
                                staffProfile.get("hireDate"),
                                filter.getHireDateFrom()
                        )
                );
            }

            if (filter.getHireDateTo() != null) {
                predicates.add(
                        cb.lessThanOrEqualTo(
                                staffProfile.get("hireDate"),
                                filter.getHireDateTo()
                        )
                );
            }

            if (filter.getStatus() != null) {
                predicates.add(
                        statusPredicate(filter.getStatus(), root, cb)
                );
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    static Predicate statusPredicate(
            StaffIdentityStatus status,
            jakarta.persistence.criteria.Root<BankUser> root,
            jakarta.persistence.criteria.CriteriaBuilder cb
    ) {
        return switch (status) {

            case CLOSED -> cb.isTrue(root.get("identityClosed"));

            case SUSPENDED ->
                    cb.and(
                            cb.isFalse(root.get("identityClosed")),
                            cb.isTrue(root.get("suspended"))
                    );

            case LOCKED ->
                    cb.and(
                            cb.isFalse(root.get("identityClosed")),
                            cb.isFalse(root.get("suspended")),
                            cb.isFalse(root.get("accountNonLocked"))
                    );

            case PENDING_ONBOARDING ->
                    cb.and(
                            cb.isFalse(root.get("identityClosed")),
                            cb.isFalse(root.get("suspended")),
                            cb.isTrue(root.get("accountNonLocked")),
                            cb.isFalse(root.get("enabled")),
                            cb.isFalse(root.get("emailVerified"))
                    );

            case DISABLED ->
                    cb.and(
                            cb.isFalse(root.get("identityClosed")),
                            cb.isFalse(root.get("suspended")),
                            cb.isTrue(root.get("accountNonLocked")),
                            cb.isFalse(root.get("enabled")),
                            cb.isTrue(root.get("emailVerified"))
                    );

            case DEACTIVATED ->
                    cb.and(
                            cb.isFalse(root.get("identityClosed")),
                            cb.isFalse(root.get("suspended")),
                            cb.isTrue(root.get("accountNonLocked")),
                            cb.isTrue(root.get("enabled")),
                            cb.isTrue(root.get("emailVerified")),
                            cb.equal(
                                    root.join("staffProfile", JoinType.INNER)
                                            .get("employmentStatus"),
                                    "TERMINATED"
                            )
                    );

            case PENDING_VERIFICATION ->
                    cb.and(
                            cb.isFalse(root.get("identityClosed")),
                            cb.isFalse(root.get("suspended")),
                            cb.isTrue(root.get("accountNonLocked")),
                            cb.isTrue(root.get("enabled")),
                            cb.isFalse(root.get("emailVerified"))
                    );

            case ACTIVE ->
                    cb.and(
                            cb.isFalse(root.get("identityClosed")),
                            cb.isFalse(root.get("suspended")),
                            cb.isTrue(root.get("accountNonLocked")),
                            cb.isTrue(root.get("enabled")),
                            cb.isTrue(root.get("emailVerified"))
                    );
        };
    }
}
