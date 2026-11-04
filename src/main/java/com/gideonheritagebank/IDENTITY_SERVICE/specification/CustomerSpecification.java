package com.gideonheritagebank.IDENTITY_SERVICE.specification;

import com.gideonheritagebank.IDENTITY_SERVICE.dto.request.CustomerFilterRequest;
import com.gideonheritagebank.IDENTITY_SERVICE.entity.BankUser;
import com.gideonheritagebank.IDENTITY_SERVICE.enums.UserType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public final class CustomerSpecification {

    private CustomerSpecification() {
    }

    public static Specification<BankUser> withFilters(
            CustomerFilterRequest filter
    ) {

        return (root, query, criteriaBuilder) -> {

            List<Predicate> predicates = new ArrayList<>();

            // Only customer identities are included.
            predicates.add(
                    criteriaBuilder.equal(
                            root.get("userType"),
                            UserType.CUSTOMER
                    )
            );

            if (filter.getEnabled() != null) {

                predicates.add(
                        criteriaBuilder.equal(
                                root.get("enabled"),
                                filter.getEnabled()
                        )
                );
            }

            if (filter.getEmailVerified() != null) {

                predicates.add(
                        criteriaBuilder.equal(
                                root.get("emailVerified"),
                                filter.getEmailVerified()
                        )
                );
            }

            if (filter.getAccountNonLocked() != null) {

                predicates.add(
                        criteriaBuilder.equal(
                                root.get("accountNonLocked"),
                                filter.getAccountNonLocked()
                        )
                );
            }

            if (filter.getCreatedFrom() != null) {

                predicates.add(
                        criteriaBuilder.greaterThanOrEqualTo(
                                root.get("createdAt"),
                                filter.getCreatedFrom()
                        )
                );
            }

            if (filter.getCreatedTo() != null) {

                predicates.add(
                        criteriaBuilder.lessThanOrEqualTo(
                                root.get("createdAt"),
                                filter.getCreatedTo()
                        )
                );
            }

            return criteriaBuilder.and(
                    predicates.toArray(new Predicate[0])
            );
        };
    }
}
