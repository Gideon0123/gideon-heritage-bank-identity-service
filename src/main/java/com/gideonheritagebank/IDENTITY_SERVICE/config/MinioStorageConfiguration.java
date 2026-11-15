package com.gideonheritagebank.IDENTITY_SERVICE.config;

import com.gideonheritagebank.IDENTITY_SERVICE.storage.MinioStorageProperties;
import io.minio.MinioClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.URI;
import java.net.URISyntaxException;

@Configuration
@EnableConfigurationProperties(MinioStorageProperties.class)
public class MinioStorageConfiguration {

    @Bean
    public MinioClient minioClient(MinioStorageProperties properties) {

        String endpoint = properties.getEndpoint();

        if (endpoint == null || endpoint.isBlank()) {
            throw new IllegalStateException(
                    "MinIO endpoint must be configured."
            );
        }

        URI endpointUri;

        try {
            endpointUri = new URI(endpoint.trim());
        } catch (URISyntaxException exception) {
            throw new IllegalStateException(
                    "Invalid MinIO endpoint: " + endpoint,
                    exception
            );
        }

        if (endpointUri.getScheme() == null
                || (!endpointUri.getScheme().equalsIgnoreCase("http")
                && !endpointUri.getScheme().equalsIgnoreCase("https"))
                || endpointUri.getHost() == null
                || endpointUri.getPort() < 1
                || endpointUri.getPort() > 65535) {

            throw new IllegalStateException(
                    "Invalid MinIO endpoint. Expected a URL such as "
                            + "http://localhost:9000, but received: "
                            + endpoint
            );
        }

        return MinioClient.builder()
                .endpoint(endpointUri.toString())
                .credentials(
                        properties.getAccessKey(),
                        properties.getSecretKey()
                )
                .build();
    }
}