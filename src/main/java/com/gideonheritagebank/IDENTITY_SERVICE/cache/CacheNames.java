package com.gideonheritagebank.IDENTITY_SERVICE.cache;

/** Central registry of Redis cache names used by the Identity Service. */
public final class CacheNames {

    private CacheNames() {
    }

    public static final String CUSTOMER_PROFILE = "customer-profile";
    public static final String CUSTOMER_ADDRESSES = "customer-addresses";
    public static final String CUSTOMER_STATUS = "customer-status";
    public static final String CUSTOMER_SEARCH = "customer-search";

    public static final String STAFF_PROFILE = "staff-profile";
    public static final String STAFF_ADDRESSES = "staff-addresses";
    public static final String STAFF_EMPLOYMENT = "staff-employment";
    public static final String STAFF_ROLE = "staff-role";
    public static final String STAFF_SEARCH = "staff-search";

    public static final String DEPARTMENTS = "departments";
}