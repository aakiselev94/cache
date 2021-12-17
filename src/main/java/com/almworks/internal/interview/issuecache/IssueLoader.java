package com.almworks.internal.interview.issuecache;

import java.util.*;
import java.util.concurrent.CompletionStage;

public interface IssueLoader {
  /**
   * <p>The returned completion stage is guaranteed to be completed in the main processing thread.</p>
   *
   * <p>For simplicity, assume that the values are always eventually downloaded without errors.</p>
   * */
  CompletionStage<? extends Result> load(Collection<Long> issueIds, Set<String> fieldIds);

  interface Result {
    /**
     * @return {@code null} if the issue was not requested
     * */
    Map<String, Object> getValues(long issueId);

    Collection<Long> getIssueIds();
    Set<String> getFieldIds();
  }
}