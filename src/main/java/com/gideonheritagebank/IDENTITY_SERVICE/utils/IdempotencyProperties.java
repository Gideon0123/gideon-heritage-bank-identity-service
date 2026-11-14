package com.gideonheritagebank.IDENTITY_SERVICE.utils;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "idempotency")
public class IdempotencyProperties {

    /** Default retention period when an annotation does not specify a TTL. */
    private long expirationMinutes = 20;
}
