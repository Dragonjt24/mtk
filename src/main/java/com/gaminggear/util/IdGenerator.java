package com.gaminggear.util;

import java.util.UUID;

/**
 * Deterministic-prefix unique ID generator for domain entities.
 */
public final class IdGenerator {

    private IdGenerator() {
    }

    public static String nextId(String prefix) {
        String shortUuid = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        return prefix + "-" + shortUuid;
    }
}
