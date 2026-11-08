package com.gideonheritagebank.IDENTITY_SERVICE.dto.request;

import com.gideonheritagebank.IDENTITY_SERVICE.enums.EmploymentStatus;
import com.gideonheritagebank.IDENTITY_SERVICE.enums.EmploymentType;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

public record UpdateStaffEmploymentRequest(

        UUID departmentId,

        UUID branchId,

        @Size(max = 120)
        String jobTitle,

        EmploymentType employmentType,

        EmploymentStatus employmentStatus,

        LocalDate effectiveFrom,

        LocalDate employmentEndDate,

        UUID reportingManagerId,

        @Size(max = 500)
        String changeReason

) {
}
