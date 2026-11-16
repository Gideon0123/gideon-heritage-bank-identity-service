package com.gideonheritagebank.IDENTITY_SERVICE.storage;

import com.gideonheritagebank.IDENTITY_SERVICE.dto.response.CustomerProfileResponse;
import com.gideonheritagebank.IDENTITY_SERVICE.dto.response.StaffProfileResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Converts the stable object key stored in BankUser into a short-lived URL
 * for API responses. The presigned URL is never persisted in PostgreSQL.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProfileImageUrlResolver {

    private static final String MANAGED_PREFIX = "profile-images/";

    private final ProfileImageStorage imageStorage;

    public String resolve(String storedValue) {
        if (storedValue == null || storedValue.isBlank()) {
            return null;
        }

        if (storedValue.startsWith(MANAGED_PREFIX)) {
            try {
                return imageStorage.createPresignedReadUrl(storedValue);
            } catch (ProfileImageStorageException exception) {
                // A storage outage should not prevent the identity profile itself
                // from being retrieved.
                log.warn("Could not create a profile image URL for object key: {}", storedValue,
                        exception);
                return null;
            }
        }

        // Preserve legacy externally hosted URLs during migration.
        if (storedValue.startsWith("https://") || storedValue.startsWith("http://")) {
            return storedValue;
        }

        return null;
    }

    public CustomerProfileResponse resolve(CustomerProfileResponse response) {
        if (response == null) {
            return null;
        }

        return new CustomerProfileResponse(
                response.id(),
                response.customerNumber(),
                response.firstName(),
                response.lastName(),
                response.email(),
                response.phoneNumber(),
                resolve(response.profilePhotoUrl()),
                response.userType(),
                response.enabled(),
                response.identityStatus(),
                response.emailVerified(),
                response.createdAt(),
                response.updatedAt()
        );
    }


    public StaffProfileResponse resolve(StaffProfileResponse response) {

        if (response == null) {
            return null;
        }

        return new StaffProfileResponse(
                response.id(),
                response.userId(),

                response.firstName(),
                response.lastName(),
                response.email(),
                response.phoneNumber(),
                resolve(response.profilePhotoUrl()),

                response.employeeNumber(),
                response.staffRole(),
                response.hireDate(),
                response.lastLoginAt(),

                response.departmentId(),
                response.departmentCode(),
                response.departmentName(),

                response.branchId(),
                response.branchCode(),
                response.branchName(),
                response.branchType(),

                response.branchAddressLine1(),
                response.branchAddressLine2(),
                response.branchCity(),
                response.branchStateOrRegion(),
                response.branchPostalCode(),
                response.branchCountryCode(),

                response.branchPhoneNumber(),
                response.branchEmail(),

                response.branchManagerId(),
                response.branchManagerName(),

                response.branchStatus(),

                response.jobTitle(),
                response.employmentType(),
                response.employmentStatus(),

                response.assignmentStartDate(),
                response.employmentEndDate(),

                response.reportingManagerId(),

                response.createdAt(),
                response.updatedAt()
        );
    }

}
