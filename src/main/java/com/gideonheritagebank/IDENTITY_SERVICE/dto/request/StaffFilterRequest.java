package com.gideonheritagebank.IDENTITY_SERVICE.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import com.gideonheritagebank.IDENTITY_SERVICE.enums.StaffIdentityStatus;
import com.gideonheritagebank.IDENTITY_SERVICE.enums.StaffRole;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
public class StaffFilterRequest {

    @Schema(description = "Enabled.")
    private Boolean enabled;

    @Schema(description = "Email verified.")
    private Boolean emailVerified;

    @Schema(description = "Account non locked.")
    private Boolean accountNonLocked;

    @Schema(description = "Suspended.")
    private Boolean suspended;

    @Schema(description = "Staff role.")
    private StaffRole staffRole;

    @Schema(description = "Status.")
    private StaffIdentityStatus status;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @Schema(description = "Created from.")
    private Instant createdFrom;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @Schema(description = "Created to.")
    private Instant createdTo;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Schema(description = "Hire date from.")
    private LocalDate hireDateFrom;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Schema(description = "Hire date to.")
    private LocalDate hireDateTo;
}