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
    private Mono<String> emptyDefaultIfEmpty;
    private Mono<String> emptyFallback;

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
        MonoVersionedMetadata.Loader<Long, String> emptyLoader = new MonoVersionedMetadata.Loader<>() {
            @Override
            public String getCached() {
                return null;
            }

            @Override
            public boolean isValid(Long version, String cached) {
                return false;
            }

            @Override
            public Mono<String> load(Long version) {
                return Mono.empty();
            }
        };
        emptyDefaultIfEmpty = MonoVersionedMetadata
            .create(Mono.empty(), emptyLoader)
            .defaultIfEmpty("empty");
        emptyFallback = MonoVersionedMetadata.create(Mono.empty(), emptyLoader, "empty");
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

    @Benchmark
    public String emptyDefaultIfEmpty() {
        return emptyDefaultIfEmpty.block();
    }

    @Benchmark
    public String emptyFallback() {
        return emptyFallback.block();
    }
}
