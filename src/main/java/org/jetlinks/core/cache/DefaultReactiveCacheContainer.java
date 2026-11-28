package org.jetlinks.core.cache;

import lombok.extern.slf4j.Slf4j;
import org.jctools.maps.NonBlockingHashMap;
import org.jetlinks.core.utils.SerialFlux;
import reactor.core.Disposable;
import reactor.core.Exceptions;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.util.context.Context;
import reactor.util.context.ContextView;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
class DefaultReactiveCacheContainer<K, V> implements ReactiveCacheContainer<K, V> {
    private final Map<K, Container<K, V>> cache = new NonBlockingHashMap<>();
    private final Map<Disposable, Retirement<K>> retiring = new IdentityHashMap<>();

    private record Loading(Container<?, ?> owner, Loading parent) { }

    private static boolean isLoading(ContextView context, Container<?, ?> owner) {
        for (Loading current = context.getOrDefault(Loading.class, null); current != null; current = current.parent) {
            if (current.owner == owner) return true;
        }
        return false;
    }

    private static final class Retirement<K> {
        private final K key;
        private final Disposable value;
        private boolean running;

        private Retirement(K key, Disposable value) {
            this.key = key;
            this.value = value;
        }
    }

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
        return Mono.deferContextual(view::read);
    }

    private final class UpdateView {
        private volatile Container<K, V> owner;
        private volatile Mono<V> initial;
        private volatile Throwable failure;

        private Mono<V> read(ContextView context) {
            Mono<V> loading = initial;
            if (loading != null) return owner != null && isLoading(context, owner) ? owner.ref() : loading;
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
        retryRetired(key, value);
        Container<K, V> previous;
        V loaded;
        synchronized (retiring) {
            transferRetirement(value);
            previous = cache.put(key, new Container<>(key, this, value));
            loaded = previous == null ? null : previous.detach();
        }
        if (previous != null) previous.notifyDetached();
        if (loaded != value) release(key, loaded);
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
        List<Retirement<K>> pending = retirements();
        V value = take(key);
        List<Throwable> failures = new ArrayList<>();
        tryRelease(key, value, failures);
        for (Retirement<K> retired : pending) {
            if (Objects.equals(key, retired.key) && retired.value != value) tryRelease(retired, failures);
        }
        throwFailures(failures);
        return value;
    }

    @Override
    public V take(K key) {
        Container<K, V> container;
        V value;
        synchronized (retiring) {
            container = cache.get(key);
            if (container == null) return null;
            transferRetirement(container.loaded);
            cache.remove(key, container);
            value = container.detach();
        }
        container.notifyDetached();
        return value;
    }

    // Caller holds retiring: ownership checks and publication must be atomic with cleanup claims.
    private void transferRetirement(Object value) {
        Retirement<K> retired = retiring.get(value);
        if (retired != null && retired.running) throw new IllegalStateException("Cache resource release is in progress");
        retiring.remove(value);
    }

    private List<Retirement<K>> retirements() {
        synchronized (retiring) { return retiring.isEmpty() ? Collections.emptyList() : new ArrayList<>(retiring.values()); }
    }

    private void retryRetired(K key, Object except) {
        List<Retirement<K>> pending = retirements();
        if (pending.isEmpty()) return;
        List<Throwable> failures = new ArrayList<>();
        for (Retirement<K> retired : pending) {
            if (Objects.equals(key, retired.key) && retired.value != except) tryRelease(retired, failures);
        }
        throwFailures(failures);
    }

    private void release(K key, Object value) {
        if (!(value instanceof Disposable)) return;
        Disposable disposable = (Disposable) value;
        Retirement<K> retired;
        Container<K, V> previous;
        synchronized (retiring) {
            Container<K, V> current = cache.get(key);
            if (current != null && (current.loaded == value || current.staged == value)) return;
            retired = retiring.computeIfAbsent(disposable, ignored -> new Retirement<>(key, disposable));
            if (retired.running) return;
            previous = claim(retired);
        }
        dispose(retired, previous);
    }

    private void release(Retirement<K> retired) {
        Container<K, V> previous;
        synchronized (retiring) {
            // A snapshot cannot reclaim cleanup transferred or completed since it was taken.
            if (retiring.get(retired.value) != retired) return;
            if (retired.running) throw new IllegalStateException("Cache resource release is in progress");
            previous = claim(retired);
        }
        dispose(retired, previous);
    }

    private Container<K, V> claim(Retirement<K> retired) {
        retired.running = true;
        Container<K, V> current = cache.get(retired.key);
        if (current != null) {
            synchronized (current) {
                if (current.loaded == retired.value) {
                    current.loaded = null;
                    return current;
                }
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private void dispose(Retirement<K> retired, Container<K, V> previous) {
        boolean completed = false;
        try {
            retired.value.dispose();
            completed = true;
        } finally {
            synchronized (retiring) {
                retired.running = false;
                if (completed) retiring.remove(retired.value, retired);
                else if (previous != null) {
                    synchronized (previous) {
                        if (!previous.detached && cache.get(retired.key) == previous && previous.loaded == null) {
                            previous.loaded = (V) retired.value;
                        }
                    }
                }
            }
        }
    }

    private void tryRelease(K key, Object value, List<Throwable> failures) {
        try { release(key, value); }
        catch (Throwable error) { Exceptions.throwIfFatal(error); failures.add(error); }
    }

    private void tryRelease(Retirement<K> retired, List<Throwable> failures) {
        try { release(retired); }
        catch (Throwable error) { Exceptions.throwIfFatal(error); failures.add(error); }
    }

    private static void throwFailures(List<Throwable> failures) {
        if (!failures.isEmpty()) throw failures.size() == 1 ? Exceptions.propagate(failures.get(0)) : Exceptions.multiple(failures);
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
        List<Retirement<K>> pending = retirements();
        List<Throwable> failures = new ArrayList<>();
        Set<Object> attempted = Collections.newSetFromMap(new IdentityHashMap<>());
        // Do not remove a replacement installed after this snapshot without disposing its own entry.
        new HashMap<>(cache).forEach((key, container) -> {
            V value;
            synchronized (retiring) {
                if (!cache.remove(key, container)) return;
                value = container.detach();
            }
            container.notifyDetached();
            attempted.add(value);
            tryRelease(key, value, failures);
        });
        for (Retirement<K> retired : pending) {
            if (!attempted.contains(retired.value)) tryRelease(retired, failures);
        }
        throwFailures(failures);
    }

    @Override
    public void dispose() { clear(); }

    private static final class Container<K, T> {
        private final DefaultReactiveCacheContainer<K, T> main;
        private final K key;
        private final SerialFlux<T> updates = new SerialFlux<>();
        private final Sinks.Empty<Void> removed = Sinks.empty();
        private volatile T loaded;
        private T staged;
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
                if (isLoading(context, this)) {
                    log.warn("recursive call reactive cache [{}]", key);
                    return Mono.fromSupplier(() -> loaded);
                }
                // Reads of an existing/in-flight load share its barrier instead of consuming update queue capacity.
                if (onlyIfAbsent && (loaded != null || pending != 0)) return ref();
                if (pending++ == 0) idle = Sinks.empty();
            }
            Mono<T> work = Mono.defer(() -> {
                    if (detached) return Mono.empty();
                    main.retryRetired(key, null);
                    if (detached) return Mono.empty();
                    T previous = loaded;
                    return onlyIfAbsent && previous != null ? Mono.just(previous) : compute.apply(previous);
                })
                .flatMap(this::replace)
                .takeUntilOther(removed.asMono())
                .doOnDiscard(Disposable.class, this::discard)
                .contextWrite(Context.of(context).put(Loading.class, new Loading(this, context.getOrDefault(Loading.class, null))));
            return updates.join(work).doOnEach(signal -> {
                if (signal.isOnComplete() || signal.isOnError()) finished(signal.getThrowable());
            });
        }

        private Mono<T> replace(T value) {
            T previous;
            boolean obsolete;
            synchronized (main.retiring) {
                synchronized (this) {
                    obsolete = detached;
                    previous = loaded;
                    if (!obsolete) {
                        main.transferRetirement(value);
                        if (previous != value) {
                            staged = value;
                            loaded = null;
                        }
                    }
                }
            }
            if (obsolete) {
                discard(value);
                return Mono.empty();
            }
            if (previous == value) return Mono.just(value);
            try {
                main.release(key, previous);
            } catch (Throwable error) {
                Exceptions.throwIfFatal(error);
                synchronized (main.retiring) {
                    synchronized (this) {
                        if (!detached) loaded = previous;
                        staged = null;
                    }
                }
                try { discard(value); }
                catch (Throwable cleanup) { Exceptions.throwIfFatal(cleanup); error = Exceptions.multiple(error, cleanup); }
                return Mono.error(error);
            }
            synchronized (main.retiring) {
                synchronized (this) {
                    obsolete = detached;
                    if (!obsolete) loaded = value;
                    staged = null;
                }
            }
            if (obsolete) {
                discard(value);
                return Mono.empty();
            }
            return Mono.just(value);
        }

        private void finished(Throwable error) {
            Sinks.Empty<Void> completed = null;
            synchronized (main.retiring) {
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
            }
            if (completed != null) {
                if (error == null) completed.tryEmitEmpty();
                else completed.tryEmitError(error);
            }
        }

        private void discard(Object value) {
            boolean owned;
            synchronized (this) {
                owned = value == loaded || value == staged || transferred != null && value == transferred.get();
            }
            if (!owned) main.release(key, value);
        }

        private T detach() {
            T value;
            synchronized (this) {
                if (detached) return null;
                detached = true;
                value = loaded;
                transferred = new WeakReference<>(value);
                loaded = null;
            }
            return value;
        }

        private void notifyDetached() {
            Sinks.Empty<Void> waiting;
            synchronized (this) { waiting = idle; idle = null; }
            removed.tryEmitEmpty();
            if (waiting != null) waiting.tryEmitEmpty();
        }

        private Mono<T> ref() {
            return Mono.deferContextual(context -> {
                Mono<Void> waiting;
                synchronized (this) {
                    if (detached) return Mono.empty();
                    if (isLoading(context, this)) {
                        return Mono.justOrEmpty(loaded);
                    }
                    waiting = idle == null ? Mono.empty() : idle.asMono();
                }
                return waiting.then(Mono.defer(() -> detached ? Mono.empty() : Mono.justOrEmpty(loaded)));
            });
        }
    }
}
