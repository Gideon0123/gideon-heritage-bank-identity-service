package com.gideonheritagebank.IDENTITY_SERVICE.mapper;

import com.gideonheritagebank.IDENTITY_SERVICE.dto.response.IdentityResponse;
import com.gideonheritagebank.IDENTITY_SERVICE.entity.BankUser;
import com.gideonheritagebank.IDENTITY_SERVICE.entity.Role;
import com.gideonheritagebank.IDENTITY_SERVICE.enums.RoleName;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface IdentityMapper {

    @Mapping(target = "roles", source = "roles")
    IdentityResponse toIdentityResponse(BankUser user);

    /*
     * MapStruct uses this method when converting each Role
     * in BankUser.roles into a RoleName in IdentityResponse.roles.
     */
    default RoleName map(Role role) {
        return role == null ? null : role.getName();
    }
}