package com.gideonheritagebank.IDENTITY_SERVICE.cache;

import java.util.Objects;
import java.util.UUID;

/**
 * Centralized cache-key construction. Use identity IDs rather than email or
 * phone number so keys remain stable when profile details change.
 */
public final class CacheKeys {

    private CacheKeys() {
    }

    public static String customerProfile(UUID customerUserId) {
        return "customer:" + requireId(customerUserId);
    }

    public static String customerAddresses(UUID customerUserId) {
        return "customer:" + requireId(customerUserId) + ":addresses";
    }

    public static String customerStatus(UUID customerUserId) {
        return "customer:" + requireId(customerUserId) + ":status";
    }

    public static String staffProfile(UUID staffUserId) {
        return "staff:" + requireId(staffUserId);
    }

    public static String staffAddresses(UUID staffUserId) {
        return "staff:" + requireId(staffUserId) + ":addresses";
    }

    public static String staffEmployment(UUID staffUserId) {
        return "staff:" + requireId(staffUserId) + ":employment";
    }

    public static String staffRole(UUID staffUserId) {
        return "staff:" + requireId(staffUserId) + ":role";
    }

    public static String department(UUID departmentId) {
        return "department:" + requireId(departmentId);
    }

    /**
     * Stable key for paginated/search results. filterFingerprint should be a
     * deterministic hash of normalized filters, not a DTO's default toString.
     */
    public static String page(
            String filterFingerprint,
            int page,
            int size,
            String sort
    ) {
        if (filterFingerprint == null || filterFingerprint.isBlank()) {
            throw new IllegalArgumentException("A filter fingerprint is required.");
        }
        if (page < 0 || size < 1) {
            throw new IllegalArgumentException("Invalid page or page size.");
        }
        String normalizedSort = sort == null || sort.isBlank() ? "unsorted" : sort.trim();
        return "page:" + filterFingerprint.trim()
                + ":p:" + page
                + ":s:" + size
                + ":sort:" + normalizedSort;
    }

    private static UUID requireId(UUID id) {
        return Objects.requireNonNull(id, "Cache key identifier must not be null.");
    }
}
