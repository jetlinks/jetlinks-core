package org.jetlinks.core.metadata;

import io.swagger.v3.oas.annotations.media.Schema;
import org.hswebframework.web.exception.I18nSupportException;
import org.jetlinks.core.annotation.Attr;
import org.jetlinks.core.annotation.Expands;
import org.jetlinks.core.utils.MetadataUtils;
import org.junit.Test;
import org.springframework.core.ResolvableType;
import reactor.core.Disposable;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

public class ExpandsMergesTest {
    @Test
    public void definitionsCanRegisterTheirOwnValueMergeFunction() {
        Map<String, Object> source = Map.of("customValueMerge", "second");
        var registration = ExpandsMerges.register("customValueMerge",
            ExpandsMerge.of((previous, addition) -> previous + ":" + addition));
        try {
            Map<String, Object> target = new HashMap<>(Map.of("customValueMerge", "first"));
            MetadataUtils.mergeExpands(target, source);
            assertEquals("first:second", target.get("customValueMerge"));
            assertEquals("second", source.get("customValueMerge"));
            Map<String, Object> absent = new HashMap<>();
            MetadataUtils.mergeExpands(absent, source);
            assertEquals(source, absent);
        } finally {
            registration.dispose();
        }
    }

    @Test
    public void spiRulesAreAvailableWithoutCallingSemanticUtilities() {
        var original = new String[]{"event", "urn:future", "event"};
        Map<String, Object> target = new HashMap<>(Map.of(
            "semanticTypes", original, "presentation", Map.of("primary", true),
            "spiOptions", Map.of("first", true)));
        MetadataUtils.mergeExpands(target, Map.of(
            "semanticTypes", List.of("urn:future", "event"),
            "presentation", Map.of("observedAt", "sampledAt"),
            "spiOptions", Map.of("second", true)));
        assertSame(original, target.get("semanticTypes"));
        assertEquals(Map.of("primary", true, "observedAt", "sampledAt"), target.get("presentation"));
        assertEquals(Map.of("first", true, "second", true), target.get("spiOptions"));
        try {
            MetadataUtils.mergeExpands(target, Map.of("presentation", Map.of("primary", false)));
            fail("Default semantic conflicts must remain visible");
        } catch (I18nSupportException expected) {
            assertTrue(expected.getMessage().contains("conflicting_metadata_semantics"));
        }
        Disposable override = ExpandsMerges.register("presentation", ExpandsMerge.OVERWRITE);
        try {
            MetadataUtils.mergeExpands(target, Map.of("presentation", Map.of("primary", false)));
            assertEquals(Map.of("primary", false), target.get("presentation"));
        } finally {
            override.dispose();
        }
        try {
            MetadataUtils.mergeExpands(target, Map.of("presentation", Map.of("primary", true)));
            fail("Disposing an override must restore the SPI rule");
        } catch (I18nSupportException expected) {
            assertTrue(expected.getMessage().contains("conflicting_metadata_semantics"));
        }
    }

    @Test
    public void registrationsCopyRulesAndRestoreAfterOutOfOrderDisposal() {
        String key = "registrationLifecycle";
        Map<String, ExpandsMerge> rules = new HashMap<>(Map.of(key, ExpandsMerge.OVERWRITE));
        Disposable first = ExpandsMerges.register(rules);
        ExpandsMerge firstSnapshot = ExpandsMerges.defaultStrategy();
        rules.put(key, ExpandsMerge.KEEP_FIRST);
        Disposable second = ExpandsMerges.register(key, ExpandsMerge.KEEP_FIRST);
        try {
            assertEquals("next", merged(firstSnapshot, key));
            assertEquals("first", merged(ExpandsMerges.defaultStrategy(), key));
            second.dispose();
            assertEquals("next", merged(ExpandsMerges.defaultStrategy(), key));
            second = ExpandsMerges.register(key, ExpandsMerge.KEEP_FIRST);
            first.dispose();
            first.dispose();
            assertTrue(first.isDisposed());
            assertEquals("first", merged(ExpandsMerges.defaultStrategy(), key));
        } finally {
            second.dispose();
            first.dispose();
        }
        assertEquals("first", merged(ExpandsMerges.defaultStrategy(), key));
        assertEquals("next", merged(firstSnapshot, key));
    }

    @Test
    public void defaultAnnotationAndTypeParsingUseRegisteredRulesWhileLocalRulesStayLocal() throws Exception {
        var field = DeclaredOptions.class.getDeclaredField("value");
        Disposable registration = ExpandsMerges.register("registeredOptions", ExpandsMerge.object(Map.of()));
        Map<String, Object> parsed;
        try {
            parsed = MetadataUtils.parseExpands(field);
            assertEquals(Map.of("caption", "first", "extra", "kept"), parsed.get("registeredOptions"));
            var model = (org.jetlinks.core.metadata.types.ObjectType) MetadataUtils.parseType(
                ResolvableType.forClass(DeclaredOptions.class));
            assertEquals(parsed.get("registeredOptions"), model.getProperty("value").orElseThrow().getExpand("registeredOptions").orElseThrow());
            assertEquals(Map.of("caption", "second"), MetadataUtils.parseExpands(field,
                ExpandsMerge.object(Map.of())).get("registeredOptions"));
        } finally {
            registration.dispose();
        }
        assertEquals(Map.of("caption", "second"), MetadataUtils.parseExpands(field).get("registeredOptions"));
        assertEquals(Map.of("caption", "first", "extra", "kept"), parsed.get("registeredOptions"));
    }

    @Test
    public void oneMergeKeepsItsSnapshotWhileAnotherThreadRegistersRules() throws Exception {
        var entered = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var executor = Executors.newSingleThreadExecutor();
        Disposable initial = ExpandsMerges.register(Map.of(
            "snapshotGate", ExpandsMerge.requireEqual((left, right) -> {
                entered.countDown();
                try {
                    if (!release.await(5, TimeUnit.SECONDS)) throw new AssertionError("Merge was not released");
                } catch (InterruptedException error) {
                    Thread.currentThread().interrupt();
                    throw new AssertionError(error);
                }
                return true;
            }, () -> new IllegalArgumentException("gate")),
            "snapshotValue", ExpandsMerge.KEEP_FIRST));
        Disposable replacement = null;
        try {
            Map<String, Object> additions = new LinkedHashMap<>();
            additions.put("snapshotGate", "next");
            additions.put("snapshotValue", "next");
            Map<String, Object> target = new HashMap<>(Map.of("snapshotGate", "first", "snapshotValue", "first"));
            var pending = executor.submit(() -> MetadataUtils.mergeExpands(target, additions));
            assertTrue(entered.await(5, TimeUnit.SECONDS));
            replacement = ExpandsMerges.register("snapshotValue", ExpandsMerge.OVERWRITE);
            release.countDown();
            pending.get(5, TimeUnit.SECONDS);
            assertEquals("first", target.get("snapshotValue"));
            assertEquals("next", merged(ExpandsMerges.defaultStrategy(), "snapshotValue"));
        } finally {
            release.countDown();
            if (replacement != null) replacement.dispose();
            initial.dispose();
            executor.shutdownNow();
        }
    }

    private static Object merged(ExpandsMerge strategy, String key) {
        Map<String, Object> target = new HashMap<>(Map.of(key, "first"));
        strategy.merge(target, Map.of(key, "next"));
        return target.get(key);
    }

    public static class SpiProvider implements ExpandsMergeProvider {
        public Map<String, ExpandsMerge> getRules() {
            return Map.of("spiOptions", ExpandsMerge.object(Map.of()));
        }
    }

    public static class DeclaredOptions {
        @Schema
        @Expands(key = "registeredOptions", value = {@Attr(key = "caption", value = "first"), @Attr(key = "extra", value = "kept")})
        @Expands(key = "registeredOptions", value = @Attr(key = "caption", value = "second"))
        private String value;
    }
}
