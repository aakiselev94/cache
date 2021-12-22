package com.almworks.internal.interview.issuecache;

import com.google.common.base.Preconditions;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static java.util.Collections.emptyMap;

public class IssueCacheImpl implements IssueCache {

  private final IssueLoader loader;
  private final Set<String> fieldIds;
  private final Map<Long, Map<String, Object>> cache;
  private final Multimap<Long, Listener> listeners;

  public IssueCacheImpl(final IssueChangeTracker tracker,
                        final IssueLoader loader,
                        final Set<String> fieldIds) {
    Preconditions.checkNotNull(tracker, "Tracker can't be null.");
    Preconditions.checkNotNull(loader, "Loader can't be null.");
    Preconditions.checkNotNull(fieldIds, "FieldIds can't be null.");
    this.loader = loader;
    this.fieldIds = fieldIds;
    this.cache = new HashMap<>();
    this.listeners = LinkedListMultimap.create();
    tracker.subscribe(issueIds -> reloadCache(issueIds, fieldIds));
  }

  @Override
  public void subscribe(final Set<Long> issueIds, final Listener listener) {
    Preconditions.checkNotNull(issueIds, "IssueIds can't be null.");
    Preconditions.checkNotNull(listener, "Listener can't be null.");
    for (final Long issueId : issueIds) {
      listeners.put(issueId, listener);
    }
    reloadCache(issueIds, fieldIds);
  }

  private void reloadCache(final Set<Long> issueIds, final Set<String> fieldIds) {
    loader.load(issueIds, fieldIds)
      .thenAccept(result -> {
        for (final Long issueId : result.getIssueIds()) {
          final Map<String, Object> values = result.getValues(issueId);
          if (values != null) {
            cache.put(issueId, values);
            listeners.get(issueId).forEach(listener -> listener.onIssueChanged(issueId, values));
          }
        }
      });
  }

  @Override
  public void unsubscribe(final Listener listener) {
    listeners.entries().removeIf(entry -> entry.getValue() == listener);
  }

  @Override
  public Object getField(long issueId, final String fieldId) {
    return cache.getOrDefault(issueId, emptyMap()).get(fieldId);
  }

  @Override
  public Set<String> getFieldIds() {
    return fieldIds;
  }

}
