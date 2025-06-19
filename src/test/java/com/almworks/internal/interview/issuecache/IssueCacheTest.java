package com.almworks.internal.interview.issuecache;

import com.google.common.base.Preconditions;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * You are expected to write unit tests for Issue Cache.
 */
public class IssueCacheTest {

    @Before
    public void setupSubscriptionTo12() {
        assertThat(myCache.getField(1L, KEY)).isNull();

        myCache.subscribe(Set.of(1L, 2L), listener12);

        assertThat(myLoader.requests).hasSize(1);

        assertThat(myCache.getField(1L, KEY)).isNull();

        myLoader.reply(Map.of(1L, issue1, 2L, issue2));

        verify(listener12).onIssueChanged(1L, issue1);
        verify(listener12).onIssueChanged(2L, issue2);
        verifyNoMoreInteractions(listener12);
        reset(listener12);

        assertThat(myCache.getField(1L, KEY)).isEqualTo("k1");
        assertThat(myCache.getField(2L, SUMMARY)).isEqualTo("s2");
    }

    @Test
    public void basic() {
        // Checks that setupSubscriptionTo12() works
    }

    @Test
    public void changeTriggersUpdate() {
        myTracker.recordChanges(Set.of(2L));

        assertThat(myLoader.requests).hasSize(1);
        checkRequests(List.of(2L));
        assertThat(myCache.getField(2L, KEY)).isEqualTo("k2");

        myLoader.reply(Map.of(2L, issue21));

        verify(listener12).onIssueChanged(2L, issue21);
        verifyNoMoreInteractions(listener12);

        assertThat(myCache.getField(2L, KEY)).isEqualTo("k21");
    }

    @Test
    public void unsubscribeListener() {
        myCache.unsubscribe(listener12);

        myCache.subscribe(Set.of(1L), listener2);
        myLoader.reply(Map.of(1L, issue1));

        verifyNoMoreInteractions(listener12);
    }

    @Test
    public void getFields() {
        assertThat(myCache.getFieldIds()).isEqualTo(FIELDS);
    }

    @Test
    public void subscribeThreeListeners() {
        myCache.unsubscribe(listener12);
        myCache.unsubscribe(listener2);
        myCache.unsubscribe(listener3);
        myCache.subscribe(Set.of(1L, 2L), listener12);
        myCache.subscribe(Set.of(1L, 2L), listener2);
        myCache.subscribe(Set.of(1L), listener3);

        assertThat(myLoader.requests).hasSize(3);
        myLoader.reply(Map.of(1L, issue1, 2L, issue2));
        assertThat(myLoader.requests).hasSize(2);
        myLoader.reply(Map.of(1L, issue1, 2L, issue2));
        assertThat(myLoader.requests).hasSize(1);
        myLoader.reply(Map.of(1L, issue1));
        assertThat(myLoader.requests).isEmpty();

        verify(listener12, times(3)).onIssueChanged(1L, issue1);
        verify(listener2, times(3)).onIssueChanged(1L, issue1);
        verify(listener3, times(3)).onIssueChanged(1L, issue1);
        verify(listener12, times(2)).onIssueChanged(2L, issue2);
        verify(listener2, times(2)).onIssueChanged(2L, issue2);
        verifyNoMoreInteractions(listener12);
        verifyNoMoreInteractions(listener2);
        verifyNoMoreInteractions(listener3);
        reset(listener12);
        reset(listener2);
        reset(listener3);

        assertThat(myCache.getField(1L, KEY)).isEqualTo("k1");
        assertThat(myCache.getField(1L, SUMMARY)).isEqualTo("s1");
        assertThat(myCache.getField(2L, KEY)).isEqualTo("k2");
        assertThat(myCache.getField(2L, SUMMARY)).isEqualTo("s2");
    }

    @SafeVarargs
    private void checkRequests(List<Long>... requests) {
        assertThat(myLoader.requests.stream().map(r -> r.issueIds).toArray()).containsExactlyInAnyOrder(Stream.of(requests).toArray());
    }

    public static final String SUMMARY = "summary";
    public static final String KEY = "key";
    public static final Set<String> FIELDS = Set.of(KEY, SUMMARY);

    private final MockIssueChangeTracker myTracker = new MockIssueChangeTracker();
    private final MockIssueLoader myLoader = new MockIssueLoader();
    private final IssueCache myCache = new IssueCacheImpl(myLoader, FIELDS, myTracker);

    private final IssueCache.Listener listener12 = mock(IssueCache.Listener.class);
    private final IssueCache.Listener listener2 = mock(IssueCache.Listener.class);
    private final IssueCache.Listener listener3 = mock(IssueCache.Listener.class);

    private final Map<String, Object> issue1 = Map.of(KEY, "k1", SUMMARY, "s1");
    private final Map<String, Object> issue2 = Map.of(KEY, "k2", SUMMARY, "s2");
    private final Map<String, Object> issue21 = Map.of(KEY, "k21", SUMMARY, "s21");

    private static class MockIssueChangeTracker implements IssueChangeTracker {

        private IssueChangeTracker.Listener myListener;

        @Override
        public void subscribe(Listener listener) {
            Preconditions.checkState(myListener == null, "unexpected second call to subscribe()");
            myListener = listener;
        }

        public void recordChanges(Set<Long> changedIds) {
            myListener.onIssuesChanged(changedIds);
        }

        @Override
        public void unsubscribe(Listener listener) {
            fail("Unexpected call to ubsubscribe()");
        }

    }


    private static class MockIssueLoader implements IssueLoader {

        public List<IssueLoadRequest> requests = new ArrayList<>();

        @Override
        public CompletionStage<LoadResult> load(Collection<Long> issueIds, Set<String> fieldIds) {
            var request = new IssueLoadRequest(issueIds, fieldIds);
            requests.add(request);
            request.resultFuture.handle((r, t) -> requests.remove(request));
            return request.resultFuture;
        }

        public void reply(Map<Long, Map<String, Object>> result) {
            assertThat(requests).isNotEmpty();
            requests.get(0).resolve(result);
        }

    }


    public static class IssueLoadRequest {

        public final List<Long> issueIds;
        public final Set<String> fieldIds;
        public final CompletableFuture<LoadResult> resultFuture = new CompletableFuture<>();

        public IssueLoadRequest(Collection<Long> issueIds, Set<String> fieldIds) {
            this.issueIds = issueIds.stream().sorted().collect(Collectors.toList());
            this.fieldIds = fieldIds;
        }

        public Collection<Long> getIssueIds() {
            return issueIds;
        }

        public Set<String> getFieldIds() {
            return fieldIds;
        }

        public CompletableFuture<LoadResult> getResultFuture() {
            return resultFuture;
        }

        public void resolve(Map<Long, Map<String, Object>> result) {
            assertThat(issueIds).containsExactlyInAnyOrder(result.keySet().toArray(new Long[ 0 ]));
            resultFuture.complete(new LoadResult(this, result));
        }

    }


    private static class LoadResult implements IssueLoader.Result {

        private final IssueLoadRequest myRequest;
        private final Map<Long, Map<String, Object>> myResult;

        private LoadResult(IssueLoadRequest req, Map<Long, Map<String, Object>> result) {
            myRequest = req;
            myResult = result;
        }

        @Override
        public Map<String, Object> getValues(long issueId) {
            return myResult.get(issueId);
        }

        @Override
        public Set<Long> getIssueIds() {
            return myResult.keySet();
        }

        @Override
        public Set<String> getFieldIds() {
            return myRequest.fieldIds;
        }

    }

}
