package com.gideonheritagebank.IDENTITY_SERVICE.entity;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class RequestFingerprint {

    private UUID userId;
    private String endpoint;
    private String queryString;
    private String httpMethod;
    private String requestBody;
}
