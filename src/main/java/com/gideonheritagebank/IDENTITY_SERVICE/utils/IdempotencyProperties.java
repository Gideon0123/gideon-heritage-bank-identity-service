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

    private long expirationMinutes = 20;

}
