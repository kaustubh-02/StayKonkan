package com.staykonkan.constant;

/**
 * General, cross-cutting application constants that don't belong to a
 * single business module (those get their own constants classes in
 * later phases, e.g. BookingConstants, CommissionConstants).
 */
public final class AppConstants {

    private AppConstants() {
    }

    public static final String API_BASE_PATH = "/api";
    public static final String API_V1 = API_BASE_PATH + "/v1";

    public static final int DEFAULT_PAGE_NUMBER = 0;
    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;

    public static final String DEFAULT_SORT_DIRECTION = "DESC";
    public static final String DEFAULT_SORT_FIELD = "createdAt";

    public static final String SYSTEM_ACTOR = "SYSTEM";
}
