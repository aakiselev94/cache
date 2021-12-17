package com.almworks.internal.interview.issuecache;

import com.google.common.base.Preconditions;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;

public class IssueCacheImpl implements IssueCache {

  private final IssueLoader loader;
  private final Set<String> fieldIds;
  private final Multimap<Long, Pair<String, Object>> cache;
  private final Multimap<Long, Listener> listeners;

  public IssueCacheImpl(final IssueChangeTracker tracker,
                        final IssueLoader loader,
                        final Set<String> fieldIds) {
    Preconditions.checkNotNull(tracker, "Tracker can't be null.");
    Preconditions.checkNotNull(loader, "Loader can't be null.");
    Preconditions.checkNotNull(fieldIds, "FieldIds can't be null.");
    this.loader = loader;
    this.fieldIds = fieldIds;
    this.cache = ArrayListMultimap.create();
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
            final List<Pair<String, Object>> pairs = values.entrySet()
                    .stream()
                    .map(e -> Pair.of(e.getKey(), e.getValue()))
                    .collect(Collectors.toList());
            cache.replaceValues(issueId, pairs);
            listeners.get(issueId).forEach(listener -> listener.onIssueChanged(issueId, values));
          }
        }
      });
  }

  @Override
  public void unsubscribe(Listener listener) {
    listeners.entries().removeIf(entry -> entry.getValue() == listener);
  }

  @Override
  public Object getField(long issueId, String fieldId) {
    return cache.asMap()
            .getOrDefault(issueId, emptyList())
            .stream()
            .filter(pair -> Objects.equals(pair.getKey(), fieldId))
            .map(Pair::getValue)
            .findFirst()
            .orElse(null);
  }

  @Override
  public Set<String> getFieldIds() {
    return fieldIds;
  }

}
