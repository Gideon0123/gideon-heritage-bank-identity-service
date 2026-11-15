package com.gideonheritagebank.IDENTITY_SERVICE.storage;

import io.minio.BucketExistsArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
@RequiredArgsConstructor
public class MinioProfileImageStorage implements ProfileImageStorage {

    private final MinioClient minioClient;
    private final MinioStorageProperties properties;

    private final AtomicBoolean bucketReady = new AtomicBoolean(false);

    @Override
    public void upload(String objectKey, MultipartFile file) {
        try {
            ensureBucketExists();

            try (InputStream inputStream = file.getInputStream()) {
                minioClient.putObject(
                        PutObjectArgs.builder()
                                .bucket(properties.getBucket())
                                .object(objectKey)
                                .stream(inputStream, file.getSize(), -1)
                                .contentType(file.getContentType())
                                .build()
                );
            }
        } catch (Exception exception) {
            throw new ProfileImageStorageException(
                    "The profile image could not be stored.",
                    exception
            );
        }
    }

    @Override
    public String createPresignedReadUrl(String objectKey) {
        try {
            ensureBucketExists();

            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(properties.getBucket())
                            .object(objectKey)
                            .expiry((int) properties.getPresignedUrlExpirySeconds())
                            .build()
            );
        } catch (Exception exception) {
            throw new ProfileImageStorageException(
                    "A temporary profile image URL could not be generated.",
                    exception
            );
        }
    }

    @Override
    public void delete(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            return;
        }

        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(properties.getBucket())
                            .object(objectKey)
                            .build()
            );
        } catch (Exception exception) {
            throw new ProfileImageStorageException(
                    "The previous profile image could not be deleted from storage.",
                    exception
            );
        }
    }

    private void ensureBucketExists() throws Exception {
        if (bucketReady.get()) {
            return;
        }

        synchronized (bucketReady) {
            if (bucketReady.get()) {
                return;
            }

            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder()
                            .bucket(properties.getBucket())
                            .build()
            );

            if (!exists) {
                try {
                    minioClient.makeBucket(
                            MakeBucketArgs.builder()
                                    .bucket(properties.getBucket())
                                    .build()
                    );
                } catch (Exception creationException) {
                    // Another application instance may have created it concurrently.
                    if (!minioClient.bucketExists(
                            BucketExistsArgs.builder()
                                    .bucket(properties.getBucket())
                                    .build()
                    )) {
                        throw creationException;
                    }
                }
            }

            bucketReady.set(true);
        }
    }
}
