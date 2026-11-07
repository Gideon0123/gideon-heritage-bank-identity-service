package com.gideonheritagebank.IDENTITY_SERVICE.dto.request;

import com.gideonheritagebank.IDENTITY_SERVICE.enums.StaffIdentityStatus;
import com.gideonheritagebank.IDENTITY_SERVICE.enums.StaffRole;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class StaffSearchRequest {

    @Size(max = 150)
    private String q;

    @Size(max = 100)
    private String name;

    @Size(max = 254)
    private String email;

    @Size(max = 30)
    private String phoneNumber;

    @Size(max = 50)
    private String employeeNumber;

    private StaffRole staffRole;

    private StaffIdentityStatus status;
}