package com.gideonheritagebank.IDENTITY_SERVICE.specification;

import com.gideonheritagebank.IDENTITY_SERVICE.dto.request.CustomerSearchRequest;
import com.gideonheritagebank.IDENTITY_SERVICE.entity.BankUser;
import com.gideonheritagebank.IDENTITY_SERVICE.enums.CustomerIdentityStatus;
import com.gideonheritagebank.IDENTITY_SERVICE.enums.UserType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class CustomerSearchSpecification {

    private CustomerSearchSpecification() {
    }

    public static Specification<BankUser> search(
            CustomerSearchRequest request
    ) {
        return (root, query, cb) -> {

            List<Predicate> predicates = new ArrayList<>();

            // Never include staff identities in customer search.
            predicates.add(cb.equal(root.get("userType"), UserType.CUSTOMER));

            /*
             * General search:
             * q searches first name, last name, email,
             * phone number, and customer number.
             */
            String q = normalize(request.getQ());

            if (q != null) {

                String pattern = "%" + q + "%";

                Predicate generalSearch = cb.or(
                        cb.like(cb.lower(root.get("firstName")), pattern),
                        cb.like(cb.lower(root.get("lastName")), pattern),
                        cb.like(
                                cb.lower(
                                        cb.concat(
                                                cb.concat(
                                                        root.get("firstName"),
                                                        " "
                                                ),
                                                root.get("lastName")
                                        )
                                ),
                                pattern
                        ),
                        cb.like(cb.lower(root.get("email")), pattern),
                        cb.like(cb.lower(root.get("phoneNumber")), pattern),
                        cb.like(cb.lower(root.get("customerNumber")), pattern)
                );

                predicates.add(generalSearch);
            }

            // Search by name.
            String name = normalize(request.getName());

            if (name != null) {

                String pattern = "%" + name + "%";

                predicates.add(
                        cb.or(
                                cb.like(
                                        cb.lower(root.get("firstName")),
                                        pattern
                                ),
                                cb.like(
                                        cb.lower(root.get("lastName")),
                                        pattern
                                ),
                                cb.like(
                                        cb.lower(
                                                cb.concat(
                                                        cb.concat(
                                                                root.get("firstName"),
                                                                " "
                                                        ),
                                                        root.get("lastName")
                                                )
                                        ),
                                        pattern
                                )
                        )
                );
            }

            // Search by email.
            String email = normalize(request.getEmail());

            if (email != null) {
                predicates.add(
                        cb.like(
                                cb.lower(root.get("email")),
                                "%" + email + "%"
                        )
                );
            }

            // Search by phone number.
            String phoneNumber = normalize(request.getPhoneNumber());

            if (phoneNumber != null) {
                predicates.add(
                        cb.like(
                                cb.lower(root.get("phoneNumber")),
                                "%" + phoneNumber + "%"
                        )
                );
            }

            // Search by customer number.
            String customerNumber = normalize(
                    request.getCustomerNumber()
            );

            if (customerNumber != null) {
                predicates.add(
                        cb.like(
                                cb.lower(root.get("customerNumber")),
                                "%" + customerNumber + "%"
                        )
                );
            }

            // Filter by derived identity status.
            if (request.getStatus() != null) {

                predicates.add(
                        statusPredicate(
                                request.getStatus(),
                                root,
                                cb
                        )
                );
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static Predicate statusPredicate(
            CustomerIdentityStatus status,
            jakarta.persistence.criteria.Root<BankUser> root,
            jakarta.persistence.criteria.CriteriaBuilder cb
    ) {

        return switch (status) {

            case LOCKED -> cb.isFalse(root.get("accountNonLocked"));

            case DISABLED ->
                    cb.and(
                            cb.isTrue(root.get("accountNonLocked")),
                            cb.isFalse(root.get("enabled"))
                    );

            case PENDING_VERIFICATION ->
                    cb.and(
                            cb.isTrue(root.get("accountNonLocked")),
                            cb.isTrue(root.get("enabled")),
                            cb.isFalse(root.get("emailVerified"))
                    );

            case ACTIVE ->
                    cb.and(
                            cb.isTrue(root.get("accountNonLocked")),
                            cb.isTrue(root.get("enabled")),
                            cb.isTrue(root.get("emailVerified"))
                    );

            case SUSPENDED ->
                    cb.and(
                            cb.isFalse(root.get("accountNonLocked")),
                            cb.isTrue(root.get("enabled"))
                    );

            case CLOSED ->
                    cb.isTrue(root.get("identityClosed"));
        };
    }

    private static String normalize(String value) {

        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim().toLowerCase(Locale.ROOT);
    }
}