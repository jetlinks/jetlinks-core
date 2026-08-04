package org.jetlinks.core.event;

import org.reactivestreams.Publisher;
import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

import java.util.function.Function;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class EventBusStructuredSubscriptionTest {

    private final EventBus legacyOnly = new EventBus() {
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
            return Mono.just(0L);
        }

        @Override
        public <T> Flux<T> subscribe(Subscription subscription, Class<T> type) {
            return Flux.empty();
        }

        @Override
        public <T> Mono<Long> publish(String topic, T event) {
            return Mono.just(0L);
        }

        @Override
        public <T> Mono<Long> publish(String topic, T event, Scheduler scheduler) {
            return Mono.just(0L);
        }
    };

    @Test
    public void shouldKeepLegacyImplementationCompatible() {
        SubscriptionPlan plan = SubscriptionPlan.builder("consumer").build();

        assertFails(
            UnsupportedOperationException.class,
            () -> legacyOnly.subscribe(plan)
        );
        assertFails(
            UnsupportedOperationException.class,
            () -> legacyOnly.subscribe(plan, payload -> Mono.empty())
        );
        assertFails(IllegalArgumentException.class, () -> legacyOnly.subscribe((SubscriptionPlan) null));
        assertFails(
            IllegalArgumentException.class,
            () -> legacyOnly.subscribe((SubscriptionPlan) null, payload -> Mono.empty())
        );
        assertFails(IllegalArgumentException.class, () -> legacyOnly.subscribe(plan, null));
    }

    private static void assertFails(Class<? extends Throwable> expected, Runnable action) {
        try {
            action.run();
            fail("expected " + expected.getName());
        } catch (Throwable error) {
            assertTrue("unexpected error: " + error, expected.isInstance(error));
        }
    }
}
