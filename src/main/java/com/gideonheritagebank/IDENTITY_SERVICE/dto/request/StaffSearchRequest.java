package com.gideonheritagebank.IDENTITY_SERVICE.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
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
    @Schema(description = "Q.")
    private String q;

    @Size(max = 100)
    @Schema(description = "Name.", example = "Lagos Main Branch")
    private String name;

    @Size(max = 254)
    @Schema(description = "Email.", example = "customer@example.com")
    private String email;

    @Size(max = 30)
    @Schema(description = "Phone number.", example = "+2348012345678")
    private String phoneNumber;

    @Size(max = 50)
    @Schema(description = "Employee number.", example = "GH-EMP-0001")
    private String employeeNumber;

    @Schema(description = "Staff role.")
    private StaffRole staffRole;

    @Schema(description = "Status.")
    private StaffIdentityStatus status;
}