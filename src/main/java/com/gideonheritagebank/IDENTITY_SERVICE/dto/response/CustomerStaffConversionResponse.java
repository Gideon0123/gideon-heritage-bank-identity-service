package com.gideonheritagebank.IDENTITY_SERVICE.dto.response;


import io.swagger.v3.oas.annotations.media.Schema;
import com.gideonheritagebank.IDENTITY_SERVICE.enums.EmploymentStatus;
import com.gideonheritagebank.IDENTITY_SERVICE.enums.StaffRole;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerStaffConversionResponse {

    @Schema(description = "Conversion id.")
    private UUID conversionId;
    @Schema(description = "User id.")
    private UUID userId;
    @Schema(description = "First name.", example = "Ada")
    private String firstName;
    @Schema(description = "Last name.", example = "Okafor")
    private String lastName;
    @Schema(description = "Email.", example = "customer@example.com")
    private String email;
    @Schema(description = "Phone number.", example = "+2348012345678")
    private String phoneNumber;
    @Schema(description = "Customer number.")
    private String customerNumber;
    @Schema(description = "Employee number.", example = "GH-EMP-0001")
    private String employeeNumber;
    @Schema(description = "Assigned staff role.")
    private StaffRole assignedStaffRole;
    @Schema(description = "Employment status.")
    private EmploymentStatus employmentStatus;
    @Schema(description = "Enabled.")
    private boolean enabled;
    @Schema(description = "Email verified.")
    private boolean emailVerified;
    @Schema(description = "Conversion reason.")
    private String conversionReason;
    @Schema(description = "Converted by user id.")
    private UUID convertedByUserId;
    @Schema(description = "Converted at.")
    private Instant convertedAt;
}
