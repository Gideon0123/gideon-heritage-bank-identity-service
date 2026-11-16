package com.gideonheritagebank.IDENTITY_SERVICE.storage;

import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProfilePhotoUrlService {

    private final MinioClient minioClient;
    private final MinioStorageProperties properties;

    public String generatePresignedUrl(String objectKey) {

        // Users without profile photos should receive null.
        if (objectKey == null || objectKey.isBlank()) {
            return null;
        }

        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(properties.getBucket())
                            .object(objectKey)
                            .expiry(
                                    (int) properties
                                            .getPresignedUrlExpirySeconds()
                            )
                            .build()
            );

        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Unable to generate profile photo URL.",
                    exception
            );
        }
    }
}

