package com.postal.utils;

public class UUIDUtils {
    public static String getUUID() {
        return java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }
}

