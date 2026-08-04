package org.jetlinks.core.topic;

import org.jetlinks.core.lang.SharedPathString;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

public class TopicSubscriptionPlanTest {

    @Test
    public void shouldMatchRoutesUsingOrSemantics() {
        TopicSubscriptionPlan plan = TopicSubscriptionPlan.of(
            PatternTopicRoute.of("/system/**"),
            IndexedTopicRoute.of(
                "/org/{orgId}/device/**",
                "orgId",
                Arrays.asList("o1", "o2")
            )
        );

        assertTrue(plan.matches(SharedPathString.of("/system/config/changed")));
        assertTrue(plan.matches(SharedPathString.of("/org/o1/device/d1/online")));
        assertFalse(plan.matches(SharedPathString.of("/org/o3/device/d1/online")));
    }

    @Test
    public void shouldBeDeterministicAndImmutable() {
        PatternTopicRoute system = PatternTopicRoute.of("/system/**");
        IndexedTopicRoute devices = IndexedTopicRoute.of(
            "/org/{orgId}/device/**",
            "orgId",
            Arrays.asList("o2", "o1")
        );
        List<TopicRoute> input = new ArrayList<>(Arrays.asList(system, devices, system));

        TopicSubscriptionPlan left = TopicSubscriptionPlan.of(input);
        TopicSubscriptionPlan right = TopicSubscriptionPlan.of(devices, system);

        input.clear();
        assertEquals(2, left.getRoutes().size());
        assertEquals(left, right);
        assertEquals(left.hashCode(), right.hashCode());
        assertEquals(left.getRoutes(), right.getRoutes());
        assertFails(UnsupportedOperationException.class, () -> left.getRoutes().clear());
    }

    @Test
    public void shouldCreatePatternRoutesFromExpandedTopics() {
        TopicSubscriptionPlan plan = TopicSubscriptionPlan.fromTopics(
            Arrays.asList("/device/{deviceId}/message", "/system/**")
        );

        assertEquals(2, plan.getRoutes().size());
        assertTrue(plan.getRoutes().contains(PatternTopicRoute.of("/device/*/message")));
        assertTrue(plan.getRoutes().contains(PatternTopicRoute.of("/system/**")));
    }

    @Test
    public void shouldRepresentEmptyPlan() {
        TopicSubscriptionPlan empty = TopicSubscriptionPlan.empty();

        assertTrue(empty.isEmpty());
        assertFalse(empty.matches(SharedPathString.of("/system/config")));
        assertSame(empty, TopicSubscriptionPlan.of(Collections.emptyList()));
        assertEquals(empty, TopicSubscriptionPlan.of(new TopicRoute[0]));
    }

    @Test
    public void shouldRejectInvalidRoutes() {
        assertFails(IllegalArgumentException.class, () -> TopicSubscriptionPlan.of((TopicRoute[]) null));
        assertFails(IllegalArgumentException.class, () -> TopicSubscriptionPlan.of((List<TopicRoute>) null));
        assertFails(
            IllegalArgumentException.class,
            () -> TopicSubscriptionPlan.of(Arrays.asList(PatternTopicRoute.of("/system/**"), null))
        );
        assertFails(IllegalArgumentException.class, () -> TopicSubscriptionPlan.fromTopics(null));
        assertFails(
            IllegalArgumentException.class,
            () -> TopicSubscriptionPlan.fromTopics(Arrays.asList("/system/**", null))
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
}
