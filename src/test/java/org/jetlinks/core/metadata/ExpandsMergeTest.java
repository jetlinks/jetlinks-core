package org.jetlinks.core.metadata;

import org.jetlinks.core.utils.MetadataUtils;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.junit.Assert.*;

public class ExpandsMergeTest {

    @Test
    public void explicitObjectsMergeWithoutRecursingIntoUnconfiguredValuesOrMutatingSources() {
        var rules = ExpandsMerge.object(Map.of("card", ExpandsMerge.object(Map.of(
            "caption", ExpandsMerge.OVERWRITE))));
        Map<String, Object> original = Map.of("caption", "first", "options", Map.of("first", true));
        Map<String, Object> addition = Map.of("caption", "second", "extra", 0,
                                             "options", Map.of("second", true));
        Map<String, Object> target = new HashMap<>(Map.of("card", original));

        MetadataUtils.mergeExpands(target, Map.of("card", addition), rules);

        assertEquals(Map.of("caption", "second", "extra", 0, "options", Map.of("first", true)), target.get("card"));
        assertEquals("first", original.get("caption"));
        assertEquals(Map.of("second", true), addition.get("options"));
    }

    @Test
    public void ordinaryValuesKeepTheirOriginalOrderAndObjectIdentity() {
        var original = new String[]{"second", "first", "second"};
        Map<String, Object> originalObject = Map.of("first", true);
        Map<String, Object> target = new HashMap<>(Map.of("events", original, "form", originalObject));
        MetadataUtils.mergeExpands(target, Map.of("events", List.of("first", "second"),
                                                 "form", Map.of("second", true)));
        assertSame(original, target.get("events"));
        assertSame(originalObject, target.get("form"));
        assertArrayEquals(new String[]{"second", "first", "second"}, original);
    }

    @Test
    public void customUnorderedRuleAcceptsArraysAndCollectionsButRetainsUnknownTermsAndOriginalValue() {
        var rules = ExpandsMerge.object(Map.of("keywords", ExpandsMerge.requireEqual(
            ExpandsMerge::unorderedEquals, () -> new IllegalArgumentException("keywords"))));
        var original = new String[]{"domain:future", "one", "one"};
        Map<String, Object> target = new HashMap<>(Map.of("keywords", original));
        MetadataUtils.mergeExpands(target, Map.of("keywords", List.of("one", "domain:future")), rules);
        assertSame(original, target.get("keywords"));
        assertConflict(() -> MetadataUtils.mergeExpands(
            target, Map.of("keywords", List.of("one", "domain:another")), rules));
        assertTrue(ExpandsMerge.unorderedEquals(new int[]{2, 1, 2}, List.of(1, 2)));
    }

    @Test
    public void orderedEqualityDoesNotIgnoreOrderOrDuplicates() {
        var rules = ExpandsMerge.object(Map.of("steps", ExpandsMerge.requireEqual(
            Objects::deepEquals, () -> new IllegalArgumentException("steps"))));
        Map<String, Object> target = new HashMap<>(Map.of("steps", new String[]{"one", "two", "one"}));
        MetadataUtils.mergeExpands(target, Map.of("steps", new String[]{"one", "two", "one"}), rules);
        assertConflict(() -> MetadataUtils.mergeExpands(
            target, Map.of("steps", new String[]{"two", "one", "one"}), rules));
        assertConflict(() -> MetadataUtils.mergeExpands(
            target, Map.of("steps", new String[]{"one", "two"}), rules));
    }

    @Test
    public void rulesAreImmutableAndScopedToTheirDeclaredObject() {
        Map<String, ExpandsMerge> fields = new HashMap<>();
        fields.put("mode", ExpandsMerge.requireEqual(Objects::deepEquals, () -> new IllegalArgumentException("mode")));
        var rules = ExpandsMerge.object(Map.of("card", ExpandsMerge.object(fields)));
        fields.clear();
        Map<String, Object> target = new HashMap<>(Map.of("mode", "first", "card", Map.of("mode", "first")));
        MetadataUtils.mergeExpands(target, Map.of("mode", "second"), rules);
        assertEquals("first", target.get("mode"));
        assertConflict(() -> MetadataUtils.mergeExpands(
            target, Map.of("card", Map.of("mode", "second")), rules));
    }

    private static void assertConflict(Runnable action) {
        reactor.test.StepVerifier.create(reactor.core.publisher.Mono.fromRunnable(action))
            .expectError(IllegalArgumentException.class).verify();
    }
}
