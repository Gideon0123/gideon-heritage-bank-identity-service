package com.gideonheritagebank.IDENTITY_SERVICE.mapper;

import com.gideonheritagebank.IDENTITY_SERVICE.dto.response.*;
import com.gideonheritagebank.IDENTITY_SERVICE.entity.*;
import com.gideonheritagebank.IDENTITY_SERVICE.enums.CustomerIdentityStatus;
import com.gideonheritagebank.IDENTITY_SERVICE.enums.RoleName;
import com.gideonheritagebank.IDENTITY_SERVICE.enums.StaffIdentityStatus;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface IdentityMapper {

    @Mapping(target = "roles", source = "roles")
    IdentityResponse toIdentityResponse(BankUser user);

    CustomerAddressResponse toCustomerAddressResponse(UserAddress address);

    @Mapping(
            target = "identityStatus",
            expression = "java(resolveCustomerIdentityStatus(user))"
    )
    CustomerProfileResponse toCustomerProfileResponse(BankUser user);

    default CustomerIdentityStatus resolveCustomerIdentityStatus(
            BankUser user
    ) {
        if (user.isIdentityClosed()) {
            return CustomerIdentityStatus.CLOSED;
        }

        if (user.isSuspended()) {
            return CustomerIdentityStatus.SUSPENDED;
        }

        if (!user.isAccountNonLocked()) {
            return CustomerIdentityStatus.LOCKED;
        }

        if (!user.isEnabled()) {
            return CustomerIdentityStatus.DISABLED;
        }

        if (!user.isEmailVerified()) {
            return CustomerIdentityStatus.PENDING_VERIFICATION;
        }

        return CustomerIdentityStatus.ACTIVE;
    }

    default RoleName map(Role role) {
        return role == null ? null : role.getName();
    }

    @Mapping(target = "customerId", source = "customer.id")
    @Mapping(target = "customerNumber", source = "customer.customerNumber")
    @Mapping(
            target = "customerName",
            expression = "java(request.getCustomer().getFirstName() + \" \" + request.getCustomer().getLastName())"
    )
    @Mapping(target = "reviewedBy", source = "reviewedBy.id")
    @Mapping(target = "completedBy", source = "completedBy.id")
    CustomerClosureResponse toCustomerClosureResponse(CustomerClosureRequest request);

    @Mapping(target = "employeeNumber", source = "staffProfile.employeeNumber")
    @Mapping(target = "staffRole", source = "staffProfile.staffRole")
    @Mapping(target = "hireDate", source = "staffProfile.hireDate")
    @Mapping(target = "lastLoginAt", source = "staffProfile.lastLoginAt")
    @Mapping(target = "identityStatus", expression = "java(resolveStaffIdentityStatus(user))")
    StaffResponse toStaffResponse(BankUser user);

    @Mapping(target = "id", source = "id")
    DepartmentResponse toDepartmentResponse(Department department);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "firstName", source = "user.firstName")
    @Mapping(target = "lastName", source = "user.lastName")
    @Mapping(target = "email", source = "user.email")
    @Mapping(target = "phoneNumber", source = "user.phoneNumber")
    @Mapping(target = "profilePhotoUrl", source = "user.profilePhotoUrl")
    @Mapping(target = "departmentId", source = "department.id")
    @Mapping(target = "departmentCode", source = "department.departmentCode")
    @Mapping(target = "departmentName", source = "department.name")
    StaffProfileResponse toStaffProfileResponse(StaffProfile staffProfile);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "departmentId", source = "department.id")
    @Mapping(target = "departmentCode", source = "departmentCodeSnapshot")
    @Mapping(target = "departmentName", source = "departmentNameSnapshot")
    StaffEmploymentHistoryResponse toStaffEmploymentHistoryResponse(
            StaffEmploymentHistory history
    );

    StaffAddressResponse toStaffAddressResponse(UserAddress address);

    default StaffIdentityStatus resolveStaffIdentityStatus(BankUser user) {

        if (user.isIdentityClosed()) {
            return StaffIdentityStatus.CLOSED;
        }

        if (user.isSuspended()) {
            return StaffIdentityStatus.SUSPENDED;
        }

        if (!user.isAccountNonLocked()) {
            return StaffIdentityStatus.LOCKED;
        }

        /*
         * A newly provisioned staff member has:
         * enabled = false
         * emailVerified = false
         *
         * They remain pending onboarding until they complete
         * the invitation and password setup process.
         */
        if (!user.isEnabled() && !user.isEmailVerified()) {
            return StaffIdentityStatus.PENDING_ONBOARDING;
        }

        if (!user.isEnabled()) {
            return StaffIdentityStatus.DISABLED;
        }

        if (!user.isEmailVerified()) {
            return StaffIdentityStatus.PENDING_VERIFICATION;
        }

        return StaffIdentityStatus.ACTIVE;
    }
}