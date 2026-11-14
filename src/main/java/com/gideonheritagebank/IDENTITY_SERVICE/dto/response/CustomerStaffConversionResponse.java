package com.gideonheritagebank.IDENTITY_SERVICE.dto.response;


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

    private UUID conversionId;
    private UUID userId;
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private String customerNumber;
    private String employeeNumber;
    private StaffRole assignedStaffRole;
    private EmploymentStatus employmentStatus;
    private boolean enabled;
    private boolean emailVerified;
    private String conversionReason;
    private UUID convertedByUserId;
    private Instant convertedAt;
}
