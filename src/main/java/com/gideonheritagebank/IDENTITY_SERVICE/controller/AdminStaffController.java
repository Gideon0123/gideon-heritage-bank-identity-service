package com.gideonheritagebank.IDENTITY_SERVICE.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import com.gideonheritagebank.IDENTITY_SERVICE.dto.request.StaffFilterRequest;
import com.gideonheritagebank.IDENTITY_SERVICE.dto.request.StaffSearchRequest;
import com.gideonheritagebank.IDENTITY_SERVICE.dto.response.ApiResponse;
import com.gideonheritagebank.IDENTITY_SERVICE.dto.response.PagedResponse;
import com.gideonheritagebank.IDENTITY_SERVICE.dto.response.StaffResponse;
import com.gideonheritagebank.IDENTITY_SERVICE.service.AdminStaffService;

import com.gideonheritagebank.IDENTITY_SERVICE.utils.TraceIdUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Staff Directory Administration", description = "Search and retrieve staff identities and staff directory records.")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/identity/admin/staff")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminStaffController {

    private final AdminStaffService adminStaffService;

    @Operation(

            summary = "Get staff by id",
            description = "Search and retrieve staff identities and staff directory records."

    )

    @ApiResponses(value = {

            @io.swagger.v3.oas.annotations.responses.ApiResponse(

                    responseCode = "200",
                    description = "Operation completed successfully."

            ),

            @io.swagger.v3.oas.annotations.responses.ApiResponse(

                    responseCode = "400",
                    description = "The request is invalid or failed validation.",
                    content = @Content(schema = @Schema(implementation = com.gideonheritagebank.IDENTITY_SERVICE.dto.response.ApiResponse.class))

            ),

            @io.swagger.v3.oas.annotations.responses.ApiResponse(

                    responseCode = "401",

                    description = "Authentication is required or the supplied access token is invalid.",

                    content = @Content(schema = @Schema(implementation = com.gideonheritagebank.IDENTITY_SERVICE.dto.response.ApiResponse.class))

            ),

            @io.swagger.v3.oas.annotations.responses.ApiResponse(

                    responseCode = "403",

                    description = "The authenticated identity is not permitted to perform this operation.",

                    content = @Content(schema = @Schema(implementation = com.gideonheritagebank.IDENTITY_SERVICE.dto.response.ApiResponse.class))

            ),

            @io.swagger.v3.oas.annotations.responses.ApiResponse(

                    responseCode = "404",

                    description = "The requested resource was not found.",

                    content = @Content(schema = @Schema(implementation = com.gideonheritagebank.IDENTITY_SERVICE.dto.response.ApiResponse.class))

            ),

            @io.swagger.v3.oas.annotations.responses.ApiResponse(

                    responseCode = "409",
                    description = "The request conflicts with the current or existing resource state.",
                    content = @Content(schema = @Schema(implementation = com.gideonheritagebank.IDENTITY_SERVICE.dto.response.ApiResponse.class))

            ),

            @io.swagger.v3.oas.annotations.responses.ApiResponse(

                    responseCode = "500",
                    description = "An unexpected server error occurred.",
                    content = @Content(schema = @Schema(implementation = com.gideonheritagebank.IDENTITY_SERVICE.dto.response.ApiResponse.class))

            )

    })

    @GetMapping("/{staffId}")
    public ResponseEntity<ApiResponse<StaffResponse>> getStaffById(
            @PathVariable UUID staffId,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request
    ) {
        StaffResponse staff = adminStaffService.getStaffById(staffId);

        ApiResponse<StaffResponse> response = ApiResponse.success(
                "Staff identity retrieved successfully.",
                HttpStatus.OK.value(),
                staff,
                request.getRequestURI(),
                getTraceId(requestId)
        );

        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(response);
    }

    @Operation(

            summary = "Get staff",
            description = "Search and retrieve staff identities and staff directory records."

    )

    @ApiResponses(value = {

            @io.swagger.v3.oas.annotations.responses.ApiResponse(

                    responseCode = "200",
                    description = "Operation completed successfully."

            ),

            @io.swagger.v3.oas.annotations.responses.ApiResponse(

                    responseCode = "400",
                    description = "The request is invalid or failed validation.",
                    content = @Content(schema = @Schema(implementation = com.gideonheritagebank.IDENTITY_SERVICE.dto.response.ApiResponse.class))

            ),

            @io.swagger.v3.oas.annotations.responses.ApiResponse(

                    responseCode = "401",
                    description = "Authentication is required or the supplied access token is invalid.",
                    content = @Content(schema = @Schema(implementation = com.gideonheritagebank.IDENTITY_SERVICE.dto.response.ApiResponse.class))

            ),

            @io.swagger.v3.oas.annotations.responses.ApiResponse(

                    responseCode = "403",
                    description = "The authenticated identity is not permitted to perform this operation.",
                    content = @Content(schema = @Schema(implementation = com.gideonheritagebank.IDENTITY_SERVICE.dto.response.ApiResponse.class))

            ),

            @io.swagger.v3.oas.annotations.responses.ApiResponse(

                    responseCode = "404",
                    description = "The requested resource was not found.",
                    content = @Content(schema = @Schema(implementation = com.gideonheritagebank.IDENTITY_SERVICE.dto.response.ApiResponse.class))

            ),

            @io.swagger.v3.oas.annotations.responses.ApiResponse(

                    responseCode = "409",
                    description = "The request conflicts with the current or existing resource state.",
                    content = @Content(schema = @Schema(implementation = com.gideonheritagebank.IDENTITY_SERVICE.dto.response.ApiResponse.class))

            ),

            @io.swagger.v3.oas.annotations.responses.ApiResponse(

                    responseCode = "500",
                    description = "An unexpected server error occurred.",
                    content = @Content(schema = @Schema(implementation = com.gideonheritagebank.IDENTITY_SERVICE.dto.response.ApiResponse.class))

            )

    })

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<StaffResponse>>> getStaff(

            @Valid @ModelAttribute StaffFilterRequest filter,
            @PageableDefault(
                    page = 1, size = 20,
                    sort = "createdAt", direction = Sort.Direction.DESC
            ) Pageable pageable,

            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request
    ) {
        Page<StaffResponse> staffPage = adminStaffService.getStaff(filter, pageable);
        PagedResponse<StaffResponse> pagedResponse = PagedResponse.from(staffPage);

        ApiResponse<PagedResponse<StaffResponse>> response = ApiResponse.success(
                "Staff records retrieved successfully.",
                HttpStatus.OK.value(),
                pagedResponse,
                request.getRequestURI(),
                getTraceId(requestId)
        );

        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(response);
    }

    @Operation(

            summary = "Search staff",
            description = "Search and retrieve staff identities and staff directory records."

    )

    @ApiResponses(value = {

            @io.swagger.v3.oas.annotations.responses.ApiResponse(

                    responseCode = "200",
                    description = "Operation completed successfully."

            ),

            @io.swagger.v3.oas.annotations.responses.ApiResponse(

                    responseCode = "400",
                    description = "The request is invalid or failed validation.",
                    content = @Content(schema = @Schema(implementation = com.gideonheritagebank.IDENTITY_SERVICE.dto.response.ApiResponse.class))

            ),

            @io.swagger.v3.oas.annotations.responses.ApiResponse(

                    responseCode = "401",
                    description = "Authentication is required or the supplied access token is invalid.",
                    content = @Content(schema = @Schema(implementation = com.gideonheritagebank.IDENTITY_SERVICE.dto.response.ApiResponse.class))

            ),

            @io.swagger.v3.oas.annotations.responses.ApiResponse(

                    responseCode = "403",
                    description = "The authenticated identity is not permitted to perform this operation.",
                    content = @Content(schema = @Schema(implementation = com.gideonheritagebank.IDENTITY_SERVICE.dto.response.ApiResponse.class))

            ),

            @io.swagger.v3.oas.annotations.responses.ApiResponse(

                    responseCode = "404",
                    description = "The requested resource was not found.",
                    content = @Content(schema = @Schema(implementation = com.gideonheritagebank.IDENTITY_SERVICE.dto.response.ApiResponse.class))

            ),

            @io.swagger.v3.oas.annotations.responses.ApiResponse(

                    responseCode = "409",
                    description = "The request conflicts with the current or existing resource state.",
                    content = @Content(schema = @Schema(implementation = com.gideonheritagebank.IDENTITY_SERVICE.dto.response.ApiResponse.class))

            ),

            @io.swagger.v3.oas.annotations.responses.ApiResponse(

                    responseCode = "500",
                    description = "An unexpected server error occurred.",
                    content = @Content(schema = @Schema(implementation = com.gideonheritagebank.IDENTITY_SERVICE.dto.response.ApiResponse.class))

            )

    })

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<PagedResponse<StaffResponse>>> searchStaff(

            @Valid @ModelAttribute StaffSearchRequest searchRequest,
            @PageableDefault(
                    page = 1, size = 20,
                    sort = "createdAt", direction = Sort.Direction.DESC
            ) Pageable pageable,

            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request
    ) {
        Page<StaffResponse> staffPage = adminStaffService.searchStaff(searchRequest, pageable);

        PagedResponse<StaffResponse> pagedResponse = PagedResponse.from(staffPage);

        ApiResponse<PagedResponse<StaffResponse>> response = ApiResponse.success(
                "Staff search completed successfully.",
                HttpStatus.OK.value(),
                pagedResponse,
                request.getRequestURI(),
                getTraceId(requestId)
        );

        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(response);
    }

    private String getTraceId(String requestId) {

        return requestId != null && !requestId.isBlank() ? requestId : TraceIdUtil.generate();
    }
}
