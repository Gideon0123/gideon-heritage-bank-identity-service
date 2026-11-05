package com.gideonheritagebank.IDENTITY_SERVICE.mapper;

import com.gideonheritagebank.IDENTITY_SERVICE.dto.response.CustomerAddressResponse;
import com.gideonheritagebank.IDENTITY_SERVICE.dto.response.CustomerProfileResponse;
import com.gideonheritagebank.IDENTITY_SERVICE.dto.response.IdentityResponse;
import com.gideonheritagebank.IDENTITY_SERVICE.entity.BankUser;
import com.gideonheritagebank.IDENTITY_SERVICE.entity.Role;
import com.gideonheritagebank.IDENTITY_SERVICE.entity.UserAddress;
import com.gideonheritagebank.IDENTITY_SERVICE.enums.CustomerIdentityStatus;
import com.gideonheritagebank.IDENTITY_SERVICE.enums.RoleName;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface IdentityMapper {

    @Mapping(target = "roles", source = "roles")
    IdentityResponse toIdentityResponse(BankUser user);

//    CustomerProfileResponse toCustomerProfileResponse(BankUser user);

    CustomerAddressResponse toCustomerAddressResponse(UserAddress address);

    @Mapping(
            target = "identityStatus",
            expression = "java(resolveCustomerIdentityStatus(user))"
    )
    CustomerProfileResponse toCustomerProfileResponse(BankUser user);

    default CustomerIdentityStatus resolveCustomerIdentityStatus(
            BankUser user
    ) {
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
}