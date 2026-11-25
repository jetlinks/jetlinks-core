package org.jetlinks.core.things;

import org.junit.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;

public class ThingsDataManagerBulkTest {
    @Test
    public void defaultHistoryHonorsHalfOpenBoundsAndMinimumTimestamp() {
        java.util.NavigableMap<Long, Integer> samples = new java.util.TreeMap<>();
        for (long time : new long[]{Long.MIN_VALUE, 5, 10, 20, 30}) samples.put(time, 1);
        ThingsDataManager manager = new ThingsDataManager() {
            @Override public Mono<ThingProperty> getLastProperty(String t, String id, String p, long time) {
                return Mono.justOrEmpty(samples.floorEntry(time)).map(value -> ThingProperty.of(p, value.getValue(), value.getKey(), null));
            }
            @Override public Mono<ThingProperty> getFirstProperty(String t, String id, String p) { return Mono.empty(); }
            @Override public Mono<Long> getLastPropertyTime(String t, String id, long time) { return Mono.empty(); }
            @Override public Mono<Long> getFirstPropertyTime(String t, String id) { return Mono.empty(); }
        };
        StepVerifier.create(manager.getProperties("device", "d", "p", 10, 30))
                    .assertNext(values -> assertEquals(java.util.Arrays.asList(20L, 10L),
                        values.stream().map(ThingProperty::getTimestamp).collect(java.util.stream.Collectors.toList())))
                    .verifyComplete();
        StepVerifier.create(manager.getProperties("device", "d", "p", 21, 30))
                    .assertNext(values -> assertEquals(0, values.size())).verifyComplete();
        StepVerifier.create(manager.getProperties("device", "d", "p", Long.MIN_VALUE, Long.MIN_VALUE + 1))
                    .assertNext(values -> assertEquals(Long.MIN_VALUE, values.get(0).getTimestamp())).verifyComplete();
    }

    @Test
    public void defaultsAreLazyAndKeepMissingAndContextSemantics() {
        AtomicInteger calls = new AtomicInteger();
        ThingsDataManager manager = new ThingsDataManager() {
            @Override public Mono<ThingProperty> getLastProperty(String t, String id, String p, long time) {
                return Mono.deferContextual(ctx -> {
                    calls.incrementAndGet();
                    return "missing".equals(p) ? Mono.empty() : Mono.just(ThingProperty.of(p, ctx.get("v"), time, "good"));
                });
            }
            @Override public Mono<ThingProperty> getFirstProperty(String t, String id, String p) { return Mono.empty(); }
            @Override public Mono<Long> getLastPropertyTime(String t, String id, long time) { return Mono.empty(); }
            @Override public Mono<Long> getFirstPropertyTime(String t, String id) { return Mono.empty(); }
        };
        Mono<List<ThingProperty>> result = manager.getLastProperties("device", "d", List.of("a", "missing", "b"), 42);
        assertEquals(0, calls.get());
        StepVerifier.create(result.contextWrite(ctx -> ctx.put("v", 9)))
                    .assertNext(values -> {
                        assertEquals(2, values.size());
                        assertEquals(9, values.get(0).getValue());
                        assertEquals(42L, values.get(1).getTimestamp());
                    }).verifyComplete();
        StepVerifier.create(result.contextWrite(ctx -> ctx.put("v", 10)))
                    .assertNext(values -> assertEquals(10, values.get(0).getValue())).verifyComplete();
        assertEquals(6, calls.get());
    }
}
