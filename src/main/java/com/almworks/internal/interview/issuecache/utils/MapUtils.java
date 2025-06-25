package com.almworks.internal.interview.issuecache.utils;

import java.util.Map;

public class MapUtils {
    private MapUtils() {}

    public static <K, V> boolean isNotEmpty(Map<K, V> map) {
        return !isEmpty(map);
    }

    public static <K, V> boolean isEmpty(Map<K, V> map) {
        return map == null || map.isEmpty();
    }
}
