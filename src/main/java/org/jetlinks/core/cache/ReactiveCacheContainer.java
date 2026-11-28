package org.jetlinks.core.cache;

import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

public interface ReactiveCacheContainer<K, V> extends Disposable {

    /**
     * Apply one lazy, shared update, serialized with this key's other accepted updates.
     * An updater error or empty result preserves an existing value; remove/take explicitly evicts it.
     * Recursive reads in the same loading chain use the current snapshot rather than waiting on themselves.
     * A replacement is committed only after the previous value is disposed successfully.
     */
    Mono<V> compute(K key, BiFunction<K, V, Mono<V>> compute);

    Mono<V> computeIfAbsent(K key, Function<K, Mono<V>> compute);

    Mono<V> get(K key, Mono<V> defaultValue);

    /**
     * Commit the new entry; failed disposal of the previous value is retained for remove/clear to retry.
     * A value currently being disposed cannot be adopted; rejection leaves the current entry intact.
     */
    V put(K key, V value);

    boolean containsKey(K key);

    V getNow(K key);

    /** Remove the value and retry this key's retained cleanup. Disposal failures remain owned by this cache. */
    V remove(K key);

    /**
     * Remove an entry and transfer ownership of its loaded value to the caller without disposing it.
     * Pending loads are cancelled; late Disposable results without a current owner are released and cannot repopulate the entry.
     * Transferred values remain protected from older loads, including after adoption under another key.
     * Any failed cleanup belonging to the transferred value is transferred too; other retired values remain owned here.
     * If cancellation fails before returning, the unreturned value stays owned here for cleanup retry.
     */
    default V take(K key) {
        throw new UnsupportedOperationException("Cache ownership transfer is not supported");
    }

    Flux<V> values();

    List<V> valuesNow();

    /** Attempt all current and retired resources despite cancellation failures, aggregate errors, and retain failed cleanup for retry. */
    void clear();

    static <K, T> ReactiveCacheContainer<K, T> create() {
        return new DefaultReactiveCacheContainer<>();
    }
}
