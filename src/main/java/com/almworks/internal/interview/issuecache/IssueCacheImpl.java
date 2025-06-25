package com.almworks.internal.interview.issuecache;

import com.almworks.internal.interview.issuecache.utils.CollectionUtils;
import com.almworks.internal.interview.issuecache.utils.MapUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletionStage;

public class IssueCacheImpl implements IssueCache, IssueChangeTracker.Listener {

    private final IssueLoader issueLoader;
    private final Set<String> fieldIds;
    private final Map<Long, Map<String, Object>> cache = new HashMap<>();
    private final Map<Listener, Set<Long>> listeners = new HashMap<>();
    private final Map<Long, Set<Listener>> issueToListeners = new HashMap<>();
    private final Map<Long, CompletionStage<Void>> inFlightRequests = new HashMap<>();

    public IssueCacheImpl(IssueLoader issueLoader, Set<String> fieldIds, IssueChangeTracker issueChangeTracker) {
        this.issueLoader = issueLoader;
        this.fieldIds = fieldIds;
        issueChangeTracker.subscribe(this);
    }

    @Override
    public void subscribe(Set<Long> issueIds, Listener listener) {
        listeners.computeIfAbsent(listener, l -> new HashSet<>()).addAll(issueIds);

        var toLoad = new HashSet<Long>();

        for (Long issueId: issueIds) {
            issueToListeners.computeIfAbsent(issueId, id -> new HashSet<>()).add(listener);

            Map<String, Object> fields = cache.get(issueId);
            if (MapUtils.isNotEmpty(fields)) {
                listener.onIssueChanged(issueId, new HashMap<>(fields));
            }

            if (!cache.containsKey(issueId) && !inFlightRequests.containsKey(issueId)) {
                toLoad.add(issueId);
            }
        }

        if (!toLoad.isEmpty()) {
            CompletionStage<Void> completion = issueLoader.load(toLoad, fieldIds).thenAccept(this::updateCacheFromResult);
            for (Long issuedId : toLoad) {
                inFlightRequests.putIfAbsent(issuedId, completion);
            }
        }
    }

    @Override
    public void unsubscribe(Listener listener) {
        Set<Long> issuedIds = listeners.remove(listener);
        if (CollectionUtils.isEmpty(issuedIds)) return;

        for (Long issuedId : issuedIds) {
            Set<Listener> issueListeners = issueToListeners.get(issuedId);
            if (CollectionUtils.isEmpty(issueListeners)) continue;

            issueListeners.remove(listener);
            if (issueListeners.isEmpty()) {
                issueToListeners.remove(issuedId);
            }
        }
    }

    @Override
    public Object getField(long issueId, String fieldId) {
        Map<String, Object> fields = cache.get(issueId);
        return MapUtils.isEmpty(fields) ? null : fields.get(fieldId);
    }

    @Override
    public Set<String> getFieldIds() {
        return fieldIds;
    }

    @Override
    public void onIssuesChanged(Set<Long> issueIds) {
        CompletionStage<Void> completion = issueLoader.load(issueIds, fieldIds).thenAccept(this::updateCacheFromResult);
        for (Long issueId : issueIds) {
            inFlightRequests.putIfAbsent(issueId, completion);
        }
    }

    private void updateCacheFromResult(IssueLoader.Result result) {
        for (Long issueId : result.getIssueIds()) {
            Map<String, Object> newFields = result.getValues(issueId);
            if (MapUtils.isEmpty(newFields)) continue;

            cache.put(issueId, new HashMap<>(newFields));
            inFlightRequests.remove(issueId);

            Set<Listener> listeners = issueToListeners.get(issueId);
            if (CollectionUtils.isEmpty(listeners)) return;

            for (Listener listener: listeners) {
                listener.onIssueChanged(issueId, new HashMap<>(newFields));
            }
        }
    }

}
