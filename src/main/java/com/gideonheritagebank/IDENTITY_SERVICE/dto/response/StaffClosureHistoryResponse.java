package com.gideonheritagebank.IDENTITY_SERVICE.dto.response;


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

    private UUID id;

    private UUID userId;

    private UUID staffProfileId;

    private String employeeNumber;

    private StaffRole finalStaffRole;

    private String closureReason;

    private UUID closedByUserId;

    private Instant closedAt;

    private Instant retentionReviewAt;
}
