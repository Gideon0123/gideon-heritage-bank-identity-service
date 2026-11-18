package com.gideonheritagebank.IDENTITY_SERVICE.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import com.gideonheritagebank.IDENTITY_SERVICE.enums.StaffRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffIdentityCreationRequest {

    @NotBlank(message = "First name is required")
    @Size(max = 100, message = "First name cannot exceed 100 characters")
    @Schema(description = "First name.", example = "Ada")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(max = 100, message = "Last name cannot exceed 100 characters")
    @Schema(description = "Last name.", example = "Okafor")
    private String lastName;

    @NotBlank(message = "Email address is required")
    @Email(message = "A valid email address is required")
    @Size(max = 254, message = "Email cannot exceed 254 characters")
    @Schema(description = "Email.", example = "customer@example.com")
    private String email;

    @NotBlank(message = "Phone number is required")
    @Pattern(
            regexp = "^\\+[1-9]\\d{7,14}$",
            message = "Phone number must use international format"
    )
    @Schema(description = "Phone number.", example = "+2348012345678")
    private String phoneNumber;

    @NotBlank(message = "Employee number is required")
    @Size(max = 50, message = "Employee number cannot exceed 50 characters")
    @Schema(description = "Employee number.", example = "GH-EMP-0001")
    private String employeeNumber;

    @NotNull(message = "Staff role is required")
    @Schema(description = "Staff role.")
    private StaffRole staffRole;

    @NotNull(message = "Hire date is required")
    @Schema(description = "Hire date.")
    private LocalDate hireDate;
}
