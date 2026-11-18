package com.gideonheritagebank.IDENTITY_SERVICE.dto.request;


import io.swagger.v3.oas.annotations.media.Schema;
import com.gideonheritagebank.IDENTITY_SERVICE.enums.StaffRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConvertCustomerToStaffRequest {

    @NotBlank(message = "Employee number is required")
    @Size(max = 50, message = "Employee number cannot exceed 50 characters")
    @Schema(description = "Employee number.", example = "GH-EMP-0001")
    private String employeeNumber;

    @NotNull(message = "Staff role is required")
    @Schema(description = "Staff role.")
    private StaffRole staffRole;

    @NotNull(message = "Hire date is required")
    @PastOrPresent(message = "Hire date cannot be in the future")
    @Schema(description = "Hire date.")
    private LocalDate hireDate;

    @NotBlank(message = "Conversion reason is required")
    @Size(max = 1000, message = "Conversion reason cannot exceed 1000 characters")
    @Schema(description = "Reason.", example = "Administrative review")
    private String reason;
}
