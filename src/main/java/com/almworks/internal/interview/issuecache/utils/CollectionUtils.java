package com.almworks.internal.interview.issuecache.utils;

import java.util.Collection;

public class CollectionUtils {
    private CollectionUtils() {

    }

    public static <T> boolean isEmpty(Collection<T> collection) {
        return collection == null || collection.isEmpty();
    }
}
