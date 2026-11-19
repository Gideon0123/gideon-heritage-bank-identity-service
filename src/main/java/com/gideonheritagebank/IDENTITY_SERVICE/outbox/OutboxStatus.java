package com.gideonheritagebank.IDENTITY_SERVICE.outbox;

public enum OutboxStatus {
    PENDING,
    PROCESSING,
    PUBLISHED,
    FAILED
}