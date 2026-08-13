package org.jetlinks.core.event;

import org.junit.Test;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.test.StepVerifier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class EventBusBatchPublishTest {

    @Test
    public void shouldUseEmptyAndSingleTopicFastPaths() {
        RecordingEventBus eventBus = new RecordingEventBus();

        StepVerifier.create(eventBus.publish(Collections.emptyList(), "value"))
                    .expectNext(0L)
                    .verifyComplete();
        assertTrue(eventBus.publishedTopics.isEmpty());

        StepVerifier.create(eventBus.publish(Collections.singletonList("/one"), "value"))
                    .expectNext(1L)
                    .verifyComplete();
        assertEquals(Collections.singletonList("/one"), eventBus.publishedTopics);
    }

    @Test
    public void shouldSnapshotTopicsAndSumLogicalCounts() {
        RecordingEventBus eventBus = new RecordingEventBus();
        List<CharSequence> topics = new ArrayList<>(Arrays.asList("/one", "/two", "/one"));

        Mono<Long> result = eventBus.publish(topics, "value");
        topics.clear();

        StepVerifier.create(result)
                    .expectNext(3L)
                    .verifyComplete();
        assertEquals(Arrays.asList("/one", "/two", "/one"), eventBus.publishedTopics);
    }

    @Test
    public void shouldConsumeSupplierOnceAndFanOutItsValue() {
        RecordingEventBus eventBus = new RecordingEventBus();
        AtomicInteger supplierCalls = new AtomicInteger();

        StepVerifier.create(eventBus.publish(
                        Arrays.asList("/one", "/two"),
                        (Supplier<String>) () -> {
                            supplierCalls.incrementAndGet();
                            return "value";
                        }))
                    .expectNext(2L)
                    .verifyComplete();

        assertEquals(1, supplierCalls.get());
        assertEquals(Arrays.asList("/one=value", "/two=value"), eventBus.received);
    }

    @Test
    public void shouldSubscribePublisherOnceAndNotSubscribeWithoutSubscribers() {
        RecordingEventBus eventBus = new RecordingEventBus();
        AtomicInteger sourceSubscriptions = new AtomicInteger();
        Publisher<String> source = Flux.defer(() -> {
            sourceSubscriptions.incrementAndGet();
            return Flux.just("a", "b");
        });

        StepVerifier.create(eventBus.publish(Arrays.asList("/one", "/two"), source))
                    .expectNext(2L)
                    .verifyComplete();
        assertEquals(1, sourceSubscriptions.get());
        List<String> received = new ArrayList<>(eventBus.received);
        Collections.sort(received);
        assertEquals(Arrays.asList("/one=a", "/one=b", "/two=a", "/two=b"), received);

        eventBus = new RecordingEventBus();
        eventBus.counts.put("/one", 0L);
        eventBus.counts.put("/two", 0L);
        AtomicInteger unusedSourceSubscriptions = new AtomicInteger();
        Publisher<String> unused = Flux.defer(() -> {
            unusedSourceSubscriptions.incrementAndGet();
            return Flux.just("unused");
        });

        StepVerifier.create(eventBus.publish(Arrays.asList("/one", "/two"), unused))
                    .expectNext(0L)
                    .verifyComplete();
        assertEquals(0, unusedSourceSubscriptions.get());
    }

    @Test
    public void shouldNotDeadlockWhenTopicCountExceedsDefaultFlatMapConcurrency() {
        RecordingEventBus eventBus = new RecordingEventBus();
        List<String> topics = new ArrayList<>();
        for (int i = 0; i < 300; i++) {
            topics.add("/topic/" + i);
        }

        StepVerifier.create(eventBus.publish(topics, Flux.just("value")))
                    .expectNext(300L)
                    .verifyComplete();
    }

    @Test
    public void shouldAllowResubscriptionOfBatchPublisher() {
        RecordingEventBus eventBus = new RecordingEventBus();
        AtomicInteger sourceSubscriptions = new AtomicInteger();
        Publisher<String> source = Flux.defer(() -> {
            sourceSubscriptions.incrementAndGet();
            return Flux.just("value");
        });

        Mono<Long> result = eventBus.publish(Arrays.asList("/one", "/two"), source);
        StepVerifier.create(result).expectNext(2L).verifyComplete();
        StepVerifier.create(result).expectNext(2L).verifyComplete();

        assertEquals(2, sourceSubscriptions.get());
    }

    @Test
    public void shouldNotStartSourceWhenEveryBranchCancelsSynchronously() {
        RecordingEventBus eventBus = new RecordingEventBus();
        eventBus.cancelPublisherBranches = true;
        AtomicInteger sourceSubscriptions = new AtomicInteger();
        Publisher<String> source = Flux.defer(() -> {
            sourceSubscriptions.incrementAndGet();
            return Flux.just("value");
        });

        StepVerifier.create(eventBus.publish(
                        Arrays.asList("/one", "/two"),
                        source
                    ))
                    // The logical count is the candidate snapshot count even when the
                    // downstream cancels before the shared source is connected.
                    .expectNext(2L)
                    .verifyComplete();

        assertEquals(0, sourceSubscriptions.get());
    }

    @Test
    public void shouldNotPendWhenSourceTerminatesSynchronously() {
        RecordingEventBus eventBus = new RecordingEventBus();

        StepVerifier.create(eventBus.publish(
                        Arrays.asList("/one", "/two"),
                        Flux.empty()
                    ))
                    .expectNext(2L)
                    .verifyComplete();
    }

    @Test
    public void shouldNotPendWhenARegistrationCancelsBeforeTheOtherBranchJoins() {
        RecordingEventBus eventBus = new RecordingEventBus();
        eventBus.cancelFirstPublisherBranch = true;
        AtomicInteger sourceSubscriptions = new AtomicInteger();
        Publisher<String> source = Flux.defer(() -> {
            sourceSubscriptions.incrementAndGet();
            return Flux.just("value");
        });

        StepVerifier.create(eventBus.publish(
                        Arrays.asList("/one", "/two"),
                        source
                    ))
                    .expectNext(1L)
                    .verifyComplete();

        assertEquals(1, sourceSubscriptions.get());
    }

    @Test
    public void shouldCancelSharedSourceWhenBatchIsCancelled() {
        RecordingEventBus eventBus = new RecordingEventBus();
        AtomicInteger sourceSubscriptions = new AtomicInteger();
        AtomicInteger sourceCancellations = new AtomicInteger();
        Publisher<String> source = Flux.defer(() -> {
            sourceSubscriptions.incrementAndGet();
            return Flux.<String>never();
        }).doOnCancel(sourceCancellations::incrementAndGet);

        Disposable disposable = eventBus.publish(
            Arrays.asList("/one", "/two"),
            source
        ).subscribe();

        assertEquals(1, sourceSubscriptions.get());
        disposable.dispose();
        assertEquals(1, sourceCancellations.get());
    }

    @Test(expected = NullPointerException.class)
    public void shouldRejectNullTopicElement() {
        new RecordingEventBus().publish(Arrays.asList("/one", null), "value");
    }

    private static final class RecordingEventBus implements EventBus {

        private final Map<String, Long> counts = new ConcurrentHashMap<>();
        private final List<String> publishedTopics = java.util.Collections.synchronizedList(new ArrayList<>());
        private final List<String> received = java.util.Collections.synchronizedList(new ArrayList<>());
        private boolean consumePublisherWithoutSubscribers;
        private boolean cancelPublisherBranches;
        private boolean cancelFirstPublisherBranch;

        private RecordingEventBus() {
            counts.put("/one", 1L);
            counts.put("/two", 1L);
        }

        @Override
        public Flux<TopicPayload> subscribe(Subscription subscription) {
            return Flux.empty();
        }

        @Override
        public Cancelable subscribe(Subscription subscription,
                                    Function<TopicPayload, Mono<Void>> handler) {
            return () -> {
            };
        }

        @Override
        public <T> Mono<Long> publish(String topic, Publisher<T> event) {
            long count = counts.getOrDefault(topic, 1L);
            if (count == 0) {
                return Mono.defer(() -> consumePublisherWithoutSubscribers
                    ? Flux.from(event).then(Mono.just(0L))
                    : Mono.just(0L));
            }
            publishedTopics.add(topic);
            if (cancelFirstPublisherBranch && "/one".equals(topic)) {
                return Mono.defer(() -> {
                    event.subscribe(new Subscriber<T>() {
                        @Override
                        public void onSubscribe(org.reactivestreams.Subscription subscription) {
                            subscription.cancel();
                        }

                        @Override
                        public void onNext(T value) {
                        }

                        @Override
                        public void onError(Throwable error) {
                        }

                        @Override
                        public void onComplete() {
                        }
                    });
                    return Mono.just(0L);
                });
            }
            if (cancelPublisherBranches) {
                return Mono.defer(() -> {
                    event.subscribe(new Subscriber<T>() {
                        @Override
                        public void onSubscribe(org.reactivestreams.Subscription subscription) {
                            subscription.cancel();
                        }

                        @Override
                        public void onNext(T value) {
                        }

                        @Override
                        public void onError(Throwable error) {
                        }

                        @Override
                        public void onComplete() {
                        }
                    });
                    return Mono.just(count);
                });
            }
            return Flux.from(event)
                       .doOnNext(value -> received.add(topic + "=" + value))
                       .then(Mono.just(count));
        }

        @Override
        public <T> Flux<T> subscribe(Subscription subscription, Class<T> type) {
            return Flux.empty();
        }

        @Override
        public <T> Mono<Long> publish(String topic, T event) {
            publishedTopics.add(topic);
            return Mono.just(counts.getOrDefault(topic, 1L));
        }

        @Override
        public <T> Mono<Long> publish(String topic, T event, Scheduler scheduler) {
            return publish(topic, event);
        }
    }
}
