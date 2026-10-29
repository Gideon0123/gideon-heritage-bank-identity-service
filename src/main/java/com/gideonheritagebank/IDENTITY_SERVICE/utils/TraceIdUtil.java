package com.gideonheritagebank.IDENTITY_SERVICE.utils;

import java.util.UUID;

public class TraceIdUtil {
    public static String generate() {
        return UUID.randomUUID().toString();
    }
}
