package org.jetlinks.core.topic;

import org.jetlinks.core.lang.SharedPathString;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

import static org.junit.Assert.*;

public class TopicRouteTest {

    @Test
    public void shouldMatchExistingWildcardSemantics() {
        PatternTopicRoute route = PatternTopicRoute.of("org/*/device/**");

        assertEquals("/org/*/device/**", route.getPattern());
        assertTrue(route.matches(SharedPathString.of("/org/o1/device")));
        assertTrue(route.matches(SharedPathString.of("org/o1/device/online")));
        assertFalse(route.matches(SharedPathString.of("/org/o1/product/p1")));
        assertFalse(route.matches(SharedPathString.of("/org/o1")));

        assertEquals(route, PatternTopicRoute.of("/org/*/device/**"));
        assertEquals(route.hashCode(), PatternTopicRoute.of("/org/*/device/**").hashCode());
        assertEquals(
            PatternTopicRoute.of("/org/*/device/**"),
            PatternTopicRoute.of("/org/*/device/**/")
        );
    }

    @Test
    public void shouldMatchTopicFinderMatrix() {
        List<String> patterns = Arrays.asList(
            "/",
            "/device",
            "/device/*",
            "/device/**",
            "/**/message",
            "/device/**/message",
            "/device/**/property/*",
            "/device/*/**",
            "/**"
        );
        List<String> topics = Arrays.asList(
            "",
            "/",
            "/device",
            "device",
            "/device/",
            "/device/d1",
            "device/d1",
            "/device/d1/message",
            "/device/group/d1/message",
            "/device/d1/property/temperature",
            "/message",
            "message",
            "/other"
        );

        for (String pattern : patterns) {
            PatternTopicRoute route = PatternTopicRoute.of(pattern);
            for (String topic : topics) {
                assertEquals(
                    pattern + " should preserve TopicFinder semantics for " + topic,
                    matchesWithTopicFinder(pattern, topic),
                    route.matches(SharedPathString.of(topic))
                );
            }
        }
    }

    @Test
    public void shouldMatchExpandedExactRoutes() {
        IndexedTopicRoute indexed = IndexedTopicRoute.of(
            "/org/{orgId}/device/**",
            "orgId",
            Arrays.asList("o1", "o2")
        );
        TopicSubscriptionPlan expanded = TopicSubscriptionPlan.of(
            PatternTopicRoute.of("/org/o1/device/**"),
            PatternTopicRoute.of("/org/o2/device/**")
        );
        List<String> topics = Arrays.asList(
            "/org/o1/device",
            "/org/o1/device/d1/online",
            "/org/o2/device/d2/message",
            "/org/o3/device/d3/online",
            "/org/o1/product/p1"
        );

        for (String topic : topics) {
            assertEquals(
                "indexed route should equal exact expansion for " + topic,
                expanded.matches(SharedPathString.of(topic)),
                indexed.matches(SharedPathString.of(topic))
            );
        }
    }

    @Test
    public void shouldRejectInvalidPattern() {
        assertInvalidPattern(null);
        assertInvalidPattern("");
        assertInvalidPattern("  ");
        assertInvalidPattern("/org//device");
        assertInvalidPattern("/org/te*st");
        assertInvalidPattern("/org/{orgId}/device");
    }

    @Test
    public void shouldMatchIndexedSegmentExactly() {
        IndexedTopicRoute route = IndexedTopicRoute.of(
            "/org/{orgId}/device/**",
            "orgId",
            Arrays.asList("o2", "o1", "o1")
        );

        assertEquals("/org/{orgId}/device/**", route.getPattern());
        assertEquals("orgId", route.getIndexedVariable());
        assertEquals(2, route.getIndexedSegment());
        assertEquals(Arrays.asList("o1", "o2"), new ArrayList<>(route.getAllowedValues()));
        assertTrue(route.matches(SharedPathString.of("/org/o1/device/online")));
        assertTrue(route.matches(SharedPathString.of("org/o2/device")));
        assertFalse(route.matches(SharedPathString.of("/org/o3/device/online")));
        assertFalse(route.matches(SharedPathString.of("/org/o1/product/p1")));
        assertFalse(route.matches(SharedPathString.of("/org")));
    }

    @Test
    public void shouldKeepIndexedRouteImmutable() {
        LinkedHashSet<String> values = new LinkedHashSet<>(Arrays.asList("o1", "o2"));
        IndexedTopicRoute route = IndexedTopicRoute.of(
            "/org/{orgId}/device/**",
            "orgId",
            values
        );

        values.add("o3");
        assertFalse(route.getAllowedValues().contains("o3"));
        assertFails(UnsupportedOperationException.class, () -> route.getAllowedValues().add("o4"));

        IndexedTopicRoute changed = route.withAllowedValues(Collections.singleton("o3"));
        assertEquals(Collections.singleton("o3"), changed.getAllowedValues());
        assertEquals(new LinkedHashSet<>(Arrays.asList("o1", "o2")), route.getAllowedValues());
        assertFalse(route.equals(changed));
    }

    @Test
    public void shouldAllowEmptyIndexedValues() {
        IndexedTopicRoute route = IndexedTopicRoute.of(
            "/org/{orgId}/device/**",
            "orgId",
            Collections.emptySet()
        );

        assertTrue(route.getAllowedValues().isEmpty());
        assertFalse(route.matches(SharedPathString.of("/org/o1/device/online")));
    }

    @Test
    public void shouldRejectInvalidIndexedRoute() {
        assertInvalidIndexed("/org/{orgId}/device/**", null, Collections.singleton("o1"));
        assertInvalidIndexed("/org/{orgId}/device/**", "", Collections.singleton("o1"));
        assertInvalidIndexed("/org/*/device/**", "orgId", Collections.singleton("o1"));
        assertInvalidIndexed("/**/org/{orgId}/device", "orgId", Collections.singleton("o1"));
        assertInvalidIndexed("/org/{orgId}/{orgId}", "orgId", Collections.singleton("o1"));
        assertInvalidIndexed("/org/{orgId}/{deviceId}", "orgId", Collections.singleton("o1"));
        assertInvalidIndexed("/org/{orgId}/device", "orgId", null);
        assertInvalidIndexed("/org/{orgId}/device", "orgId", Collections.singleton(null));
        assertInvalidIndexed("/org/{orgId}/device", "orgId", Collections.singleton(""));
        assertInvalidIndexed("/org/{orgId}/device", "orgId", Collections.singleton("a/b"));
        assertInvalidIndexed("/org/{orgId}/device", "orgId", Collections.singleton("*"));
        assertInvalidIndexed("/org/{orgId}/device", "orgId", Collections.singleton("**"));
        assertInvalidIndexed("/org/{orgId}/device", "orgId", Collections.singleton("{other}"));
    }

    private static void assertInvalidPattern(String pattern) {
        assertFails(IllegalArgumentException.class, () -> PatternTopicRoute.of(pattern));
    }

    private static void assertInvalidIndexed(String pattern,
                                             String variable,
                                             Iterable<? extends CharSequence> values) {
        assertFails(
            IllegalArgumentException.class,
            () -> IndexedTopicRoute.of(
                pattern,
                variable,
                values == null ? null : toCollection(values)
            )
        );
    }

    private static java.util.Collection<? extends CharSequence> toCollection(
        Iterable<? extends CharSequence> values) {
        java.util.List<CharSequence> copy = new ArrayList<>();
        values.forEach(copy::add);
        return copy;
    }

    private static boolean matchesWithTopicFinder(String pattern, String topic) {
        Topic<String> root = Topic.createRoot();
        Topic<String> subscribed = root.append(pattern);
        subscribed.subscribe("subscriber");
        List<Topic<String>> matched = new ArrayList<>();
        TopicFinder.find(root, topic, matched::add, () -> {
        });
        return matched.contains(subscribed);
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
