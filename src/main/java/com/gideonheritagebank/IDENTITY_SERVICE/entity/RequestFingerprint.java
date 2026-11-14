package com.gideonheritagebank.IDENTITY_SERVICE.entity;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RequestFingerprint {

    private Long userId;
    private String endpoint;
    private String httpMethod;
    private String requestBody;
}