package com.gideonheritagebank.IDENTITY_SERVICE.specification;

import com.gideonheritagebank.IDENTITY_SERVICE.dto.request.StaffFilterRequest;
import com.gideonheritagebank.IDENTITY_SERVICE.dto.request.StaffSearchRequest;
import com.gideonheritagebank.IDENTITY_SERVICE.entity.BankUser;
import com.gideonheritagebank.IDENTITY_SERVICE.entity.StaffProfile;
import com.gideonheritagebank.IDENTITY_SERVICE.enums.StaffIdentityStatus;
import com.gideonheritagebank.IDENTITY_SERVICE.enums.UserType;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;

import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class StaffSearchSpecification {

    private StaffSearchSpecification() {
    }

    public static Specification<BankUser> search(
            StaffSearchRequest request
    ) {

        return (root, query, cb) -> {

            List<Predicate> predicates = new ArrayList<>();

            predicates.add(
                    cb.equal(root.get("userType"), UserType.STAFF)
            );

            Join<BankUser, StaffProfile> staffProfile =
                    root.join("staffProfile", JoinType.INNER);

            /*
             * General search across:
             * first name, last name, full name,
             * email, phone number, employee number.
             */
            String q = normalize(request.getQ());

            if (q != null) {

                String pattern = "%" + q + "%";

                Predicate generalSearch = cb.or(

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
                        ),

                        cb.like(
                                cb.lower(root.get("email")),
                                pattern
                        ),

                        cb.like(
                                cb.lower(root.get("phoneNumber")),
                                pattern
                        ),

                        cb.like(
                                cb.lower(
                                        staffProfile.get("employeeNumber")
                                ),
                                pattern
                        )
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

            // Search by employee number.
            String employeeNumber = normalize(
                    request.getEmployeeNumber()
            );

            if (employeeNumber != null) {
                predicates.add(
                        cb.like(
                                cb.lower(
                                        staffProfile.get("employeeNumber")
                                ),
                                "%" + employeeNumber + "%"
                        )
                );
            }

            // Filter by staff position.
            if (request.getStaffRole() != null) {
                predicates.add(
                        cb.equal(
                                staffProfile.get("staffRole"),
                                request.getStaffRole()
                        )
                );
            }

            // Filter by identity status.
            if (request.getStatus() != null) {
                predicates.add(
                        StaffSpecification.statusPredicate(
                                request.getStatus(), root, cb
                        )
                );
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static StaffFilterRequest createStatusFilter(
            StaffIdentityStatus status
    ) {
        StaffFilterRequest filter = new StaffFilterRequest();
        filter.setStatus(status);
        return filter;
    }

    private static String normalize(String value) {

        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim().toLowerCase(Locale.ROOT);
    }
}
