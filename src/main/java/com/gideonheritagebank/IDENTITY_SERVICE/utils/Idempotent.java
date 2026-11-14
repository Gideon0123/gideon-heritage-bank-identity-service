package com.gideonheritagebank.IDENTITY_SERVICE.utils;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Idempotent {

    long ttlMinutes() default 30;

}