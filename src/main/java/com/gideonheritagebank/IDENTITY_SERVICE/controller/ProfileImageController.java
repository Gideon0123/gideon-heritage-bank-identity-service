package com.gideonheritagebank.IDENTITY_SERVICE.controller;

import com.gideonheritagebank.IDENTITY_SERVICE.dto.response.ApiResponse;
import com.gideonheritagebank.IDENTITY_SERVICE.dto.response.ProfileImageResponse;
import com.gideonheritagebank.IDENTITY_SERVICE.storage.ProfileImageService;
import com.gideonheritagebank.IDENTITY_SERVICE.utils.TraceIdUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.gideonheritagebank.IDENTITY_SERVICE.utils.Idempotent;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/identity/profile-image")
@PreAuthorize("isAuthenticated()")
public class ProfileImageController {

    private final ProfileImageService profileImageService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Idempotent(ttlMinutes = 3)
    public ResponseEntity<ApiResponse<ProfileImageResponse>> upload(
            @RequestPart("file") MultipartFile file,
            HttpServletRequest request
    ) {
        ProfileImageResponse response =
                profileImageService.uploadMyProfileImage(file);

        return success(
                "Profile image uploaded successfully.",
                response,
                HttpStatus.OK,
                request
        );
    }

    @GetMapping
    public ResponseEntity<ApiResponse<ProfileImageResponse>> get(
            HttpServletRequest request
    ) {
        ProfileImageResponse response =
                profileImageService.getMyProfileImage();

        return success(
                "Profile image URL generated successfully.",
                response,
                HttpStatus.OK,
                request
        );
    }

    @DeleteMapping
    @Idempotent(ttlMinutes = 3)
    public ResponseEntity<ApiResponse<Void>> delete(
            HttpServletRequest request
    ) {
        profileImageService.deleteMyProfileImage();

        return success(
                "Profile image deleted successfully.",
                null,
                HttpStatus.NO_CONTENT,
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

        ApiResponse<T> body = ApiResponse.success(
                message,
                status.value(),
                data,
                request.getRequestURI(),
                requestId
        );

        return ResponseEntity.status(status)
                .cacheControl(CacheControl.noStore())
                .body(body);
    }
}
