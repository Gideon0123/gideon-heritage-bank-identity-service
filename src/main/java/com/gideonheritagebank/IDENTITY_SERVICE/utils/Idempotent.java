package com.gideonheritagebank.IDENTITY_SERVICE.utils;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Idempotent {

    /**
     * TTL in minutes. A value less than or equal to zero uses the configured
     * idempotency.expiration-minutes default.
     */
    long ttlMinutes() default 0;
}