package com.gideonheritagebank.IDENTITY_SERVICE.dto.response;


import io.swagger.v3.oas.annotations.media.Schema;
import com.gideonheritagebank.IDENTITY_SERVICE.enums.StaffRole;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffClosureHistoryResponse {

    @Schema(description = "Id.")
    private UUID id;

    @Schema(description = "User id.")
    private UUID userId;

    @Schema(description = "Staff profile id.")
    private UUID staffProfileId;

    @Schema(description = "Employee number.", example = "GH-EMP-0001")
    private String employeeNumber;

    @Schema(description = "Final staff role.")
    private StaffRole finalStaffRole;

    @Schema(description = "Closure reason.")
    private String closureReason;

    @Schema(description = "Closed by user id.")
    private UUID closedByUserId;

    @Schema(description = "Closed at.")
    private Instant closedAt;

    @Schema(description = "Retention review at.")
    private Instant retentionReviewAt;
}
