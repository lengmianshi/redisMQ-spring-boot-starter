package com.leng.project.redisqueue.utils;

import java.util.UUID;

public class StringUtils {

    public static String getUUID() {
        return UUID.randomUUID().toString().replaceAll("-", "");
    }
}
