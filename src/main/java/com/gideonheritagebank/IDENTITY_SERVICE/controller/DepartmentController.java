package com.gideonheritagebank.IDENTITY_SERVICE.controller;

import com.gideonheritagebank.IDENTITY_SERVICE.dto.request.CreateDepartmentRequest;
import com.gideonheritagebank.IDENTITY_SERVICE.dto.request.UpdateDepartmentRequest;
import com.gideonheritagebank.IDENTITY_SERVICE.dto.response.ApiResponse;
import com.gideonheritagebank.IDENTITY_SERVICE.dto.response.DepartmentResponse;
import com.gideonheritagebank.IDENTITY_SERVICE.dto.response.PagedResponse;
import com.gideonheritagebank.IDENTITY_SERVICE.service.DepartmentService;
import com.gideonheritagebank.IDENTITY_SERVICE.utils.TraceIdUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/identity/admin/departments")
@PreAuthorize("hasRole('ADMIN')")
public class DepartmentController {

    private final DepartmentService departmentService;

    @PostMapping
    public ResponseEntity<ApiResponse<DepartmentResponse>> createDepartment(
            @Valid @RequestBody CreateDepartmentRequest requestBody,
            HttpServletRequest request
    ) {
        DepartmentResponse department = departmentService.createDepartment(requestBody);

        return success(
                "Department created successfully.",
                department,
                HttpStatus.CREATED,
                request
        );
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<DepartmentResponse>>> getDepartments(
            @RequestParam(required = false) Boolean active,
            @PageableDefault(
                    page = 0,
                    size = 20,
                    sort = "name",
                    direction = Sort.Direction.ASC
            ) Pageable pageable,
            HttpServletRequest request
    ) {

        PagedResponse<DepartmentResponse> departments = departmentService.getDepartments(active, pageable);


        return success(
                "Departments retrieved successfully.",
                departments,
                HttpStatus.OK,
                request
        );
    }

    @GetMapping("/{departmentId}")
    public ResponseEntity<ApiResponse<DepartmentResponse>> getDepartmentById(
            @PathVariable UUID departmentId,
            HttpServletRequest request
    ) {
        DepartmentResponse department = departmentService.getDepartmentById(departmentId);

        return success(
                "Department retrieved successfully.",
                department,
                HttpStatus.OK,
                request
        );
    }

    @PatchMapping("/{departmentId}")
    public ResponseEntity<ApiResponse<DepartmentResponse>> updateDepartment(
            @PathVariable UUID departmentId,
            @Valid @RequestBody UpdateDepartmentRequest requestBody,
            HttpServletRequest request
    ) {

        DepartmentResponse department = departmentService.updateDepartment(
                departmentId,
                requestBody
        );

        return success(
                "Department updated successfully.",
                department,
                HttpStatus.OK,
                request
        );
    }

    @PatchMapping("/{departmentId}/activate")
    public ResponseEntity<ApiResponse<DepartmentResponse>> activateDepartment(
            @PathVariable UUID departmentId,
            HttpServletRequest request
    ) {
        DepartmentResponse department =
                departmentService.activateDepartment(departmentId);

        return success(
                "Department activated successfully.",
                department,
                HttpStatus.OK,
                request
        );
    }

    @PatchMapping("/{departmentId}/deactivate")
    public ResponseEntity<ApiResponse<DepartmentResponse>> deactivateDepartment(
            @PathVariable UUID departmentId,
            HttpServletRequest request
    ) {
        DepartmentResponse department =
                departmentService.deactivateDepartment(departmentId);

        return success(
                "Department deactivated successfully.",
                department,
                HttpStatus.OK,
                request
        );
    }

    private <T> ResponseEntity<ApiResponse<T>> success(
            String message,
            T data,
            HttpStatus status,
            HttpServletRequest request
    ) {

        String requestId = request.getHeader("X-Request-Id");

        if (requestId == null || requestId.isBlank()) {
            requestId = TraceIdUtil.generate();
        }

        ApiResponse<T> response = ApiResponse.success(
                message,
                status.value(),
                data,
                request.getRequestURI(),
                requestId
        );

        return ResponseEntity.status(status)
                .cacheControl(CacheControl.noStore())
                .body(response);
    }
}
