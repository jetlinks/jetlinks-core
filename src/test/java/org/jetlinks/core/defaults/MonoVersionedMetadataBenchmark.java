package org.jetlinks.core.defaults;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import reactor.core.publisher.Mono;

import java.util.concurrent.TimeUnit;

@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class MonoVersionedMetadataBenchmark {

    private Mono<String> plain;
    private Mono<String> cacheHit;
    private Mono<String> versionLoad;
    private Mono<String> emptyLoad;

    @Setup
    public void setup() {
        plain = Mono.just("value");
        cacheHit = MonoVersionedMetadata.create(
            Mono.just(1L),
            () -> "cached",
            (version, cached) -> version == 1L,
            version -> Mono.just("loaded"),
            Mono::empty
        );
        versionLoad = MonoVersionedMetadata.create(
            Mono.just(2L),
            () -> "cached",
            (version, cached) -> version == 1L,
            version -> Mono.just("loaded"),
            Mono::empty
        );
        emptyLoad = MonoVersionedMetadata.create(
            Mono.empty(),
            () -> "cached",
            (version, cached) -> false,
            version -> Mono.just("loaded"),
            () -> Mono.just("empty")
        );
    }

    @Benchmark
    public String plainMono() {
        return plain.block();
    }

    @Benchmark
    public String cacheHit() {
        return cacheHit.block();
    }

    @Benchmark
    public String versionLoad() {
        return versionLoad.block();
    }

    @Benchmark
    public String emptyLoad() {
        return emptyLoad.block();
    }
}
