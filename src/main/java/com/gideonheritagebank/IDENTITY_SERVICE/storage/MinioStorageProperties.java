package com.gideonheritagebank.IDENTITY_SERVICE.storage;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "storage.minio")
public class MinioStorageProperties {

    @NotBlank
    private String endpoint = "http://localhost:9000";

    @NotBlank
    private String accessKey = "minioadmin";

    @NotBlank
    private String secretKey = "minioadmin";

    @NotBlank
    private String bucket = "ghb-profile-images";

    @Min(1)
    @Max(5242880)
    private long maxFileSizeBytes = 5 * 1024 * 1024;

    @Min(1)
    @Max(604800)
    private long presignedUrlExpirySeconds = 900;
}
