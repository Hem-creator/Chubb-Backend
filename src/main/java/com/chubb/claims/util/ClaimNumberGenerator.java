package com.chubb.claims.util;

import java.util.UUID;

public final class ClaimNumberGenerator {

    private static final String PREFIX = "CLM-";

    private ClaimNumberGenerator() {
    }

    public static String generate() {
        return PREFIX + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }
}
