package com.gideonheritagebank.IDENTITY_SERVICE.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
public class CustomerFilterRequest {

    @Schema(description = "Enabled.")
    private Boolean enabled;

    @Schema(description = "Email verified.")
    private Boolean emailVerified;

    @Schema(description = "Account non locked.")
    private Boolean accountNonLocked;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @Schema(description = "Created from.")
    private Instant createdFrom;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @Schema(description = "Created to.")
    private Instant createdTo;

}
