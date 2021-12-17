package com.almworks.internal.interview.issuecache;

import java.util.Map;
import java.util.Set;

/**
 * Issue cache downloads and holds values of Jira issue fields.
 * The cache is live: it notifies its users when issues change.
 * To receive notifications, users {@link #subscribe} to updates on a set of issues.
 * When issues change, all users subscribed to changed issues receive new field values.
 * */
public interface IssueCache {
  /**
   * <p>Subscribes to updates on the specified issues.
   * Whenever any issue from the specified set is updated,
   * {@code listener} is called in the main processing thread
   * with the ID of the changed issue and the new values for all fields
   * specified in {@code fieldIds}.</p>
   *
   * <p>If at the moment of subscription some issues are stored in the cache,
   * the {@code listener} should receive their fields immediately.</p>
   *
   * <p>This method is called in the main processing thread.</p>
   * */
  void subscribe(Set<Long> issueIds, Listener listener);

  /**
   * <p>Unsubscribes the specified listener from updates.</p>
   *
   * <p>This method is called in the main processing thread.</p>
   * */
  void unsubscribe(Listener listener);

  /**
   * Returns the current stored field value for the specified {@code issueId}.
   * This method does not cause any requests to Jira, it only returns what's already in the cache.
   * @param issueId an issue ID; if no values are stored in the cache, {@code null} is returned
   * @param fieldId String ID of the field; if this cache doesn't store the field, {@code null} is returned
   * @return Currently stored field value for the specified issue or {@code null}
   * */
  Object getField(long issueId, String fieldId);

  /**
   * The set of fields that will cache will request and store for each issue.
   *
   * @return The set of fields that will cache will request and store for each issue
   * */
  Set<String> getFieldIds();

  interface Listener {
    /**
     * This method is called when issue changes with the new field values.
     * The implementation might conflate several updates into one, in which case only the last values must be reported.
     * @param issueId the ID of the issue
     * @param fieldValues updated field values - for each field in {@link IssueCache#getFieldIds()}
     * */
    void onIssueChanged(long issueId, Map<String, Object> fieldValues);
  }
}
