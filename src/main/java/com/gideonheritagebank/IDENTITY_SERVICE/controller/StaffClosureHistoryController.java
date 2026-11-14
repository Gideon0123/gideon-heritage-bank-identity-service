package com.gideonheritagebank.IDENTITY_SERVICE.controller;


import com.gideonheritagebank.IDENTITY_SERVICE.dto.response.ApiResponse;
import com.gideonheritagebank.IDENTITY_SERVICE.dto.response.PagedResponse;
import com.gideonheritagebank.IDENTITY_SERVICE.dto.response.StaffClosureHistoryResponse;
import com.gideonheritagebank.IDENTITY_SERVICE.service.StaffLifecycleService;
import com.gideonheritagebank.IDENTITY_SERVICE.utils.TraceIdUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/identity/admin/staff/closure-history")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class StaffClosureHistoryController {

    private final StaffLifecycleService staffLifecycleService;

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<StaffClosureHistoryResponse>>> getStaffClosureHistory(
            @PageableDefault(
                    page = 0,
                    size = 20,
                    sort = "closedAt",
                    direction = Sort.Direction.DESC
            ) Pageable pageable,
            HttpServletRequest request
    ) {
        Page<StaffClosureHistoryResponse> history =
                staffLifecycleService.getStaffClosureHistory(pageable);

        PagedResponse<StaffClosureHistoryResponse> response = PagedResponse.from(history);

        return success(
                "Staff Closure history retrieved successfully.",
                response,
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

