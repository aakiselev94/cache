package com.almworks.internal.interview.issuecache;

import java.util.Set;

public interface IssueChangeTracker {
  void subscribe(Listener listener);

  void unsubscribe(Listener listener);

  interface Listener {
    void onIssuesChanged(Set<Long> issueIds);
  }
}

