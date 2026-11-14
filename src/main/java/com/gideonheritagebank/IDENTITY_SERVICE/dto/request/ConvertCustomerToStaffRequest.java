package com.gideonheritagebank.IDENTITY_SERVICE.dto.request;


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
    private String employeeNumber;

    @NotNull(message = "Staff role is required")
    private StaffRole staffRole;

    @NotNull(message = "Hire date is required")
    @PastOrPresent(message = "Hire date cannot be in the future")
    private LocalDate hireDate;

    @NotBlank(message = "Conversion reason is required")
    @Size(max = 1000, message = "Conversion reason cannot exceed 1000 characters")
    private String reason;
}
