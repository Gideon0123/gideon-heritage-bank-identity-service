package com.gideonheritagebank.IDENTITY_SERVICE.storage;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "storage.minio")
public class MinioStorageProperties {

    @NotBlank
    private String endpoint = "http://localhost:9000";

    @NotBlank
    @Value("${storage.minio.access-key}")
    private String accessKey;

    @NotBlank
    @Value("${storage.minio.secret-key}")
    private String secretKey;

    @NotBlank
    @Value("${storage.minio.bucket}")
    private String bucket;

    @Min(1)
    @Max(5242880)
    private long maxFileSizeBytes = 5 * 1024 * 1024;

    @Min(1)
    @Max(604800)
    private long presignedUrlExpirySeconds = 900;
}
