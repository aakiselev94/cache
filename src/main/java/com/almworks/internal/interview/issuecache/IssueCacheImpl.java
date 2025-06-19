package com.almworks.internal.interview.issuecache;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class IssueCacheImpl implements IssueCache, IssueChangeTracker.Listener {

    private final IssueLoader issueLoader;
    private final Set<String> fieldIds;
    private final Map<Long, Map<String, Object>> cache = new HashMap<>();
    private final Map<Listener, Set<Long>> listeners = new HashMap<>();
    private final Map<Long, Set<Listener>> issueToListeners = new HashMap<>();

    public IssueCacheImpl(IssueLoader issueLoader, Set<String> fieldIds, IssueChangeTracker issueChangeTracker) {
        this.issueLoader = issueLoader;
        this.fieldIds = fieldIds;
        issueChangeTracker.subscribe(this);
    }

    @Override
    public synchronized void subscribe(Set<Long> issueIds, Listener listener) {
        listeners.computeIfAbsent(listener, l -> new HashSet<>()).addAll(issueIds);

        for (Long issueId: issueIds) {
            issueToListeners.computeIfAbsent(issueId, id -> new HashSet<>()).add(listener);
        }

        for (Long issueId: issueIds) {
            Map<String, Object> fields = cache.get(issueId);
            if (fields != null && !fields.isEmpty()) {
                listener.onIssueChanged(issueId, new HashMap<>(fields));
            }
        }

        var toLoad = new HashSet<Long>();
        for (Long issueId : issueIds) {
            if (!cache.containsKey(issueId)) {
                toLoad.add(issueId);
            }
        }

        if (!toLoad.isEmpty()) {
            issueLoader.load(toLoad, fieldIds).thenAccept(this::updateCacheFromResult);
        }
    }

    @Override
    public synchronized void unsubscribe(Listener listener) {
        Set<Long> issuedIds = listeners.remove(listener);
        if (issuedIds != null) {
            for (Long issuedId : issuedIds) {
                Set<Listener> ls = issueToListeners.get(issuedId);
                if (ls != null) {
                    ls.remove(listener);
                    if (ls.isEmpty()) {
                        issueToListeners.remove(issuedId);
                    }
                }
            }
        }
    }

    @Override
    public synchronized Object getField(long issueId, String fieldId) {
        Map<String, Object> fields = cache.get(issueId);
        if (fields == null || !fieldIds.contains(fieldId)) {
            return null;
        }
        return fields.get(fieldId);
    }

    @Override
    public Set<String> getFieldIds() {
        return fieldIds;
    }

    @Override
    public void onIssuesChanged(Set<Long> issueIds) {
        issueLoader.load(issueIds, fieldIds).thenAccept(this::updateCacheFromResult);
    }

    private synchronized void updateCacheFromResult(IssueLoader.Result result) {
        for (Long issueId : result.getIssueIds()) {
            Map<String, Object> newFields = result.getValues(issueId);
            if (newFields == null) continue;

            cache.put(issueId, new HashMap<>(newFields));

            Set<Listener> ls = issueToListeners.get(issueId);
            if (ls != null) {
                for (Listener listener: ls) {
                    listener.onIssueChanged(issueId, new HashMap<>(newFields));
                }
            }
        }
    }

}
