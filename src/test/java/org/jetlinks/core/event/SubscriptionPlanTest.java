package org.jetlinks.core.event;

import org.jetlinks.core.topic.IndexedTopicRoute;
import org.jetlinks.core.topic.PatternTopicRoute;
import org.jetlinks.core.topic.TopicSubscriptionPlan;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.junit.Assert.*;

public class SubscriptionPlanTest {

    @Test
    public void shouldBuildImmutablePlan() {
        Subscription.Feature[] features = {
            Subscription.Feature.broker,
            Subscription.Feature.local
        };
        SubscriptionPlan plan = SubscriptionPlan.builder("device-message-consumer")
            .routes(TopicSubscriptionPlan.of(PatternTopicRoute.of("/device/**")))
            .features(features)
            .priority(100)
            .time(1234L)
            .build();

        features[0] = Subscription.Feature.shared;
        assertEquals("device-message-consumer", plan.getSubscriber());
        assertEquals(2, plan.getFeatures().size());
        assertTrue(plan.getFeatures().contains(Subscription.Feature.broker));
        assertEquals(100, plan.getPriority());
        assertEquals(1234L, plan.getTime());
        assertFails(
            UnsupportedOperationException.class,
            () -> plan.getFeatures().add(Subscription.Feature.shared)
        );
    }

    @Test
    public void shouldPreserveCallbacksWhenRoutesChange() {
        AtomicInteger subscribed = new AtomicInteger();
        AtomicInteger dropped = new AtomicInteger();
        TopicPayload payload = TopicPayload.of("/device/d1/online", "online");

        SubscriptionPlan original = SubscriptionPlan.builder("device-message-consumer")
            .doOnSubscribe(subscribed::incrementAndGet)
            .onDropped(ignore -> dropped.incrementAndGet())
            .build();
        SubscriptionPlan changed = original.withRoutes(
            TopicSubscriptionPlan.of(
                IndexedTopicRoute.of(
                    "/org/{orgId}/device/**",
                    "orgId",
                    Collections.singleton("o1")
                )
            )
        );

        changed.subscribed();
        changed.discard(payload);
        changed.dropped(payload);

        assertEquals(1, subscribed.get());
        assertEquals(2, dropped.get());
        assertSame(changed, changed.withRoutes(changed.getRoutePlan()));
    }

    @Test
    public void shouldConvertLegacySubscription() {
        AtomicInteger subscribed = new AtomicInteger();
        AtomicInteger dropped = new AtomicInteger();
        Subscription legacy = Subscription.builder()
            .subscriberId("legacy-device-consumer")
            .topics("/device/{deviceId}/message")
            .justBroker()
            .priority(10)
            .time(100L)
            .doOnSubscribe(subscribed::incrementAndGet)
            .onDropped(ignore -> dropped.incrementAndGet())
            .build();

        SubscriptionPlan plan = SubscriptionPlan.from(legacy);

        assertEquals("legacy-device-consumer", plan.getSubscriber());
        assertEquals(Collections.singleton(Subscription.Feature.broker), plan.getFeatures());
        assertEquals(10, plan.getPriority());
        assertEquals(100L, plan.getTime());
        assertTrue(
            plan.getRoutePlan().getRoutes().contains(
                PatternTopicRoute.of("/device/*/message")
            )
        );
        plan.subscribed();
        plan.dropped(TopicPayload.of("/device/d1/message", "data"));
        assertEquals(1, subscribed.get());
        assertEquals(1, dropped.get());
    }

    @Test
    public void shouldExcludeCallbacksFromValueSemantics() {
        SubscriptionPlan left = SubscriptionPlan.builder("consumer")
            .routes(TopicSubscriptionPlan.of(PatternTopicRoute.of("/device/**")))
            .doOnSubscribe(() -> {
            })
            .onDropped(ignore -> {
            })
            .build();
        SubscriptionPlan right = SubscriptionPlan.builder("consumer")
            .routes(TopicSubscriptionPlan.of(PatternTopicRoute.of("/device/**")))
            .doOnSubscribe(() -> {
                throw new AssertionError("must not participate in equality");
            })
            .onDropped(ignore -> {
                throw new AssertionError("must not participate in equality");
            })
            .build();

        assertEquals(left, right);
        assertEquals(left.hashCode(), right.hashCode());
        assertFalse(left.toString().contains("callback"));
    }

    @Test
    public void shouldExposeRouteOnlyUpdateCompatibility() {
        Runnable subscribed = () -> {
        };
        Consumer<TopicPayload> dropped = ignore -> {
        };
        SubscriptionPlan original = SubscriptionPlan.builder("consumer")
            .routes(TopicSubscriptionPlan.of(PatternTopicRoute.of("/device/**")))
            .features(Subscription.Feature.local, Subscription.Feature.shared)
            .priority(10)
            .time(100L)
            .doOnSubscribe(subscribed)
            .onDropped(dropped)
            .build();

        assertTrue(original.isUpdateCompatibleWith(original.withRoutes(
            TopicSubscriptionPlan.of(PatternTopicRoute.of("/system/**"))
        )));
        assertFalse(original.isUpdateCompatibleWith(copyOf(original, "other", subscribed, dropped)));
        assertFalse(original.isUpdateCompatibleWith(
            SubscriptionPlan.builder("consumer")
                .features(Subscription.Feature.local)
                .priority(10)
                .time(100L)
                .doOnSubscribe(subscribed)
                .onDropped(dropped)
                .build()
        ));
        assertFalse(original.isUpdateCompatibleWith(
            SubscriptionPlan.builder("consumer")
                .features(Subscription.Feature.local, Subscription.Feature.shared)
                .priority(11)
                .time(100L)
                .doOnSubscribe(subscribed)
                .onDropped(dropped)
                .build()
        ));
        assertFalse(original.isUpdateCompatibleWith(
            SubscriptionPlan.builder("consumer")
                .features(Subscription.Feature.local, Subscription.Feature.shared)
                .priority(10)
                .time(101L)
                .doOnSubscribe(subscribed)
                .onDropped(dropped)
                .build()
        ));
        assertFalse(original.isUpdateCompatibleWith(copyOf(original, "consumer", () -> {
        }, dropped)));
        assertFalse(original.isUpdateCompatibleWith(copyOf(original, "consumer", subscribed, ignore -> {
        })));
        assertFails(NullPointerException.class, () -> original.isUpdateCompatibleWith(null));
    }

    @Test
    public void shouldValidateBuilderInput() {
        assertFails(IllegalArgumentException.class, () -> SubscriptionPlan.builder(null).build());
        assertFails(IllegalArgumentException.class, () -> SubscriptionPlan.builder(" ").build());
        assertFails(
            IllegalArgumentException.class,
            () -> SubscriptionPlan.builder("consumer").features((Subscription.Feature[]) null)
        );
        assertFails(
            IllegalArgumentException.class,
            () -> SubscriptionPlan.builder("consumer")
                .features(Arrays.asList(Subscription.Feature.local, null)
                                .toArray(new Subscription.Feature[0]))
        );

        SubscriptionPlan defaultPlan = SubscriptionPlan.builder("consumer").build();
        assertEquals(Collections.singleton(Subscription.Feature.local), defaultPlan.getFeatures());
        assertTrue(defaultPlan.getRoutePlan().isEmpty());
    }

    @Test
    public void shouldExposeUpdateResultAsValue() {
        SubscriptionUpdateResult result = new SubscriptionUpdateResult(
            3,
            true,
            SubscriptionSynchronization.CLUSTER_SYNCHRONIZED
        );

        assertEquals(3, result.getRevision());
        assertTrue(result.isChanged());
        assertEquals(
            SubscriptionSynchronization.CLUSTER_SYNCHRONIZED,
            result.getSynchronization()
        );
        assertEquals(
            result,
            new SubscriptionUpdateResult(
                3,
                true,
                SubscriptionSynchronization.CLUSTER_SYNCHRONIZED
            )
        );
        assertFails(
            IllegalArgumentException.class,
            () -> new SubscriptionUpdateResult(
                -1,
                false,
                SubscriptionSynchronization.LOCAL_APPLIED
            )
        );
        assertFails(
            NullPointerException.class,
            () -> new SubscriptionUpdateResult(0, false, null)
        );
    }

    private static void assertFails(Class<? extends Throwable> expected, Runnable action) {
        try {
            action.run();
            fail("expected " + expected.getName());
        } catch (Throwable error) {
            assertTrue("unexpected error: " + error, expected.isInstance(error));
        }
    }

    private static SubscriptionPlan copyOf(SubscriptionPlan source,
                                           String subscriber,
                                           Runnable subscribed,
                                           Consumer<TopicPayload> dropped) {
        return SubscriptionPlan.builder(subscriber)
            .routes(source.getRoutePlan())
            .features(source.getFeatures().toArray(new Subscription.Feature[0]))
            .priority(source.getPriority())
            .time(source.getTime())
            .doOnSubscribe(subscribed)
            .onDropped(dropped)
            .build();
    }
}
