package org.jetlinks.core.cache;

import lombok.extern.slf4j.Slf4j;
import org.jctools.maps.NonBlockingHashMap;
import org.jetlinks.core.utils.SerialFlux;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.util.context.Context;
import reactor.util.context.ContextView;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
class DefaultReactiveCacheContainer<K, V> implements ReactiveCacheContainer<K, V> {
    private final Map<K, Container<K, V>> cache = new NonBlockingHashMap<>();

    @Override
    public Mono<V> compute(K key, BiFunction<K, V, Mono<V>> compute) {
        return update(key, old -> compute.apply(key, old), false);
    }

    @Override
    public Mono<V> computeIfAbsent(K key, Function<K, Mono<V>> compute) {
        return update(key, old -> compute.apply(key), true);
    }

    private Mono<V> update(K key, Function<V, Mono<V>> compute, boolean onlyIfAbsent) {
        UpdateView view = new UpdateView();
        view.initial = Mono.deferContextual(context -> {
            for (;;) {
                Container<K, V> container = cache.computeIfAbsent(key, id -> new Container<>(id, this, null));
                Mono<V> operation = container.update(compute, onlyIfAbsent, context);
                if (operation != null) {
                    view.owner = container;
                    return operation;
                }
            }
        }).doOnError(error -> view.failure = error).doFinally(signal -> {
            if (view.owner != null) view.initial = null;
        }).cache();
        return Mono.defer(view::read);
    }

    private final class UpdateView {
        private volatile Container<K, V> owner;
        private volatile Mono<V> initial;
        private volatile Throwable failure;

        private Mono<V> read() {
            Mono<V> loading = initial;
            if (loading != null) return loading;
            if (failure != null) return Mono.error(failure);
            // The completed source is released before later subscriptions read the live entry.
            return owner.ref();
        }
    }

    @Override
    public Mono<V> get(K key, Mono<V> defaultValue) {
        Container<K, V> container = cache.get(key);
        return container == null ? defaultValue : container.ref().switchIfEmpty(defaultValue);
    }

    @Override
    public V put(K key, V value) {
        Container<K, V> previous = cache.put(key, new Container<>(key, this, value));
        V loaded = previous == null ? null : previous.detach();
        if (loaded != value) disposeValue(loaded);
        return loaded;
    }

    @Override
    public boolean containsKey(K key) { return cache.containsKey(key); }

    @Override
    public V getNow(K key) {
        Container<K, V> container = cache.get(key);
        return container == null ? null : container.loaded;
    }

    @Override
    public V remove(K key) {
        V value = take(key);
        disposeValue(value);
        return value;
    }

    @Override
    public V take(K key) {
        Container<K, V> container = cache.remove(key);
        return container == null ? null : container.detach();
    }

    private static void disposeValue(Object value) {
        if (value instanceof Disposable) ((Disposable) value).dispose();
    }

    @Override
    public Flux<V> values() {
        return Flux.fromIterable(cache.values()).flatMap(Container::ref);
    }

    @Override
    public List<V> valuesNow() {
        return cache.values().stream().map(container -> container.loaded)
            .filter(java.util.Objects::nonNull).collect(Collectors.toList());
    }

    @Override
    public void clear() {
        // Do not remove a replacement installed after this snapshot without disposing its own entry.
        new HashMap<>(cache).forEach((key, container) -> {
            if (cache.remove(key, container)) container.dispose();
        });
    }

    @Override
    public void dispose() { clear(); }

    private static final class Container<K, T> implements Disposable {
        private final DefaultReactiveCacheContainer<K, T> main;
        private final K key;
        private final SerialFlux<T> updates = new SerialFlux<>();
        private final Sinks.Empty<Void> removed = Sinks.empty();
        private volatile T loaded;
        private volatile WeakReference<T> transferred;
        private volatile boolean detached;
        private int pending;
        private Sinks.Empty<Void> idle;

        private Container(K key, DefaultReactiveCacheContainer<K, T> main, T loaded) {
            this.key = key;
            this.main = main;
            this.loaded = loaded;
        }

        private Mono<T> update(Function<T, Mono<T>> compute, boolean onlyIfAbsent, ContextView context) {
            synchronized (this) {
                if (detached || main.cache.get(key) != this) return null;
                if (context.getOrDefault(DefaultReactiveCacheContainer.class, null) == this) {
                    log.warn("recursive call reactive cache [{}]", key);
                    return Mono.fromSupplier(() -> loaded);
                }
                // Reads of an existing/in-flight load share its barrier instead of consuming update queue capacity.
                if (onlyIfAbsent && (loaded != null || pending != 0)) return ref();
                if (pending++ == 0) idle = Sinks.empty();
            }
            Mono<T> work = Mono.defer(() -> {
                    if (detached) return Mono.empty();
                    T previous = loaded;
                    return onlyIfAbsent && previous != null ? Mono.just(previous) : compute.apply(previous);
                })
                .flatMap(this::replace)
                .takeUntilOther(removed.asMono())
                .doOnDiscard(Disposable.class, this::discard)
                .contextWrite(Context.of(context).put(DefaultReactiveCacheContainer.class, this));
            return updates.join(work.flux()).singleOrEmpty().doOnEach(signal -> {
                if (signal.isOnComplete() || signal.isOnError()) finished(signal.getThrowable());
            });
        }

        private Mono<T> replace(T value) {
            T previous;
            boolean obsolete;
            synchronized (this) {
                obsolete = detached;
                previous = loaded;
                if (!obsolete) loaded = value;
            }
            if (obsolete) {
                discard(value);
                return Mono.empty();
            }
            if (previous != value) disposeValue(previous);
            return Mono.just(value);
        }

        private void finished(Throwable error) {
            Sinks.Empty<Void> completed = null;
            synchronized (this) {
                if (--pending == 0) {
                    completed = idle;
                    idle = null;
                    // Failed/empty first loads are evicted; a failed update never loses its old value.
                    if (loaded == null && !detached) {
                        detached = true;
                        main.cache.remove(key, this);
                    }
                }
            }
            if (completed != null) {
                if (error == null) completed.tryEmitEmpty();
                else completed.tryEmitError(error);
            }
        }

        private void discard(Object value) {
            boolean owned;
            synchronized (this) {
                owned = value == loaded || transferred != null && value == transferred.get();
            }
            if (!owned) disposeValue(value);
        }

        private T detach() {
            T value;
            Sinks.Empty<Void> waiting;
            synchronized (this) {
                if (detached) return null;
                detached = true;
                value = loaded;
                transferred = new WeakReference<>(value);
                loaded = null;
                waiting = idle;
                idle = null;
            }
            removed.tryEmitEmpty();
            if (waiting != null) waiting.tryEmitEmpty();
            return value;
        }

        private Mono<T> ref() {
            return Mono.deferContextual(context -> {
                Mono<Void> waiting;
                synchronized (this) {
                    if (detached) return Mono.empty();
                    if (context.getOrDefault(DefaultReactiveCacheContainer.class, null) == this) {
                        return Mono.justOrEmpty(loaded);
                    }
                    waiting = idle == null ? Mono.empty() : idle.asMono();
                }
                return waiting.then(Mono.defer(() -> detached ? Mono.empty() : Mono.justOrEmpty(loaded)));
            });
        }

        @Override
        public void dispose() { disposeValue(detach()); }
    }
}
