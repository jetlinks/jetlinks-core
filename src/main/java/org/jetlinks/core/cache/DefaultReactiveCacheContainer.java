package org.jetlinks.core.cache;

import lombok.extern.slf4j.Slf4j;
import org.jctools.maps.NonBlockingHashMap;
import org.jetlinks.core.utils.Reactors;
import reactor.core.Disposable;
import reactor.core.Disposables;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.util.context.Context;
import reactor.util.context.ContextView;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
class DefaultReactiveCacheContainer<K, V> implements ReactiveCacheContainer<K, V> {

    private final Map<K, Container<K, V>> cache = new NonBlockingHashMap<>();

    @Override
    public Mono<V> compute(K key, BiFunction<K, V, Mono<V>> compute) {

        return cache
            .compute(key, (k, old) -> {
                if (old == null) {
                    Mono<V> loader = compute.apply(k, null);
                    return new Container<>(
                        k,
                        DefaultReactiveCacheContainer.this,
                        loader);
                }
                old.update(loaded -> compute.apply(k, loaded));
                return old;
            })
            .ref();
    }

    @Override
    public Mono<V> computeIfAbsent(K key, Function<K, Mono<V>> compute) {
        return cache
            .computeIfAbsent(key, k -> {
                Mono<V> loader = compute.apply(k);
                return new Container<>(
                    k,
                    DefaultReactiveCacheContainer.this,
                    loader);
            })
            .ref();
    }

    @Override
    public Mono<V> get(K key, Mono<V> defaultValue) {
        Container<K, V> container = cache.get(key);
        if (container != null) {
            return container.ref().switchIfEmpty(defaultValue);
        }
        return defaultValue;
    }

    @Override
    public V put(K key, V value) {
        Container<K, V> container = cache.put(key, new Container<>(key, this, value));
        if (container != null) {
            container.dispose();
            return container.loaded;
        }
        return null;
    }

    @Override
    public boolean containsKey(K key) {
        return cache.containsKey(key);
    }

    @Override
    public V getNow(K key) {
        Container<K, V> container = cache.get(key);
        if (container != null) {
            return container.loaded;
        }
        return null;
    }

    @Override
    public V remove(K key) {
        Container<K, V> container = cache.remove(key);
        if (null != container) {
            container.dispose();
        }
        return container == null ? null : container.loaded;
    }

    @Override
    public Flux<V> values() {
        return Flux
            .fromIterable(cache.values())
            .flatMap(Container::ref);
    }

    @Override
    public List<V> valuesNow() {
        return cache
            .values()
            .stream()
            .filter(c -> c.loaded != null)
            .map(c -> c.loaded)
            .collect(Collectors.toList());
    }

    @Override
    public void clear() {
        Map<K, Container<K, V>> cache = new HashMap<>(this.cache);
        this.cache.clear();
        for (Container<K, V> value : cache.values()) {
            value.dispose();
        }
    }

    @Override
    public void dispose() {
        cache.values().forEach(Container::dispose);
        cache.clear();
    }


    static class Container<K, T> implements Disposable {
        @SuppressWarnings("rawtypes")
        private final static AtomicReferenceFieldUpdater<Container, Mono> LOADER
            = AtomicReferenceFieldUpdater.newUpdater(Container.class, Mono.class, "loader");
        protected volatile Mono<T> loader;

        @SuppressWarnings("rawtypes")
        private final static AtomicReferenceFieldUpdater<Container, Sinks.One> AWAIT
            = AtomicReferenceFieldUpdater.newUpdater(Container.class, Sinks.One.class, "await");
        private volatile Sinks.One<T> await;

        @SuppressWarnings("rawtypes")
        private final static AtomicReferenceFieldUpdater<Container, Object> LOADED
            = AtomicReferenceFieldUpdater.newUpdater(Container.class, Object.class, "loaded");
        public volatile T loaded;

        private final Disposable.Swap disposable = Disposables.swap();
        private final DefaultReactiveCacheContainer<K, T> main;
        private final K key;

        public Container(K key, DefaultReactiveCacheContainer<K, T> main, Mono<T> loader) {
            this.key = key;
            this.main = main;
            update(ignore -> loader);
        }

        public Container(K key, DefaultReactiveCacheContainer<K, T> main, T loaded) {
            this.key = key;
            this.main = main;
            this.loaded = loaded;
            update(ignore -> Mono.just(loaded));
        }

        public void update(Function<T, Mono<T>> ref) {
            synchronized (this) {
                @SuppressWarnings("all")
                Mono<T> loader = LOADER.getAndSet(this, null);
                if (loader != null) {
                    loader = loader.flatMap(ref);
                } else {
                    @SuppressWarnings("all")
                    Sinks.One<T> await = AWAIT.get(this);
                    if (await != null) {
                        loader = await.asMono().flatMap(ref);
                    } else {
                        loader = ref.apply(this.loaded);
                    }
                }
                AWAIT.compareAndSet(this, null, Sinks.one());
                LOADER.set(this, loader);
            }
        }

        private void afterLoaded(T data) {
            if (data != loaded && loaded instanceof Disposable) {
                ((Disposable) loaded).dispose();
            }
            loaded = data;
            @SuppressWarnings("unchecked")
            Sinks.One<T> await = AWAIT.getAndSet(this, null);
            if (await != null) {
                await.emitValue(data, Reactors.emitFailureHandler());
            }
        }


        public void dispose() {
            disposable.dispose();
            T loaded = this.loaded;
            if (loaded instanceof Disposable) {
                ((Disposable) loaded).dispose();
            }
        }

        private void loadError(Throwable err) {
            @SuppressWarnings("all")
            Sinks.One<T> await = AWAIT.getAndSet(this, null);
            if (await != null) {
                await.emitError(err, Reactors.emitFailureHandler());
            }
            main.cache.remove(key, this);
        }

        private void loadEmpty() {
            @SuppressWarnings("all")
            Sinks.One<T> await = AWAIT.getAndSet(this, null);
            if (await != null) {
                await.emitEmpty(Reactors.emitFailureHandler());
            }
            //加载结果为空,移除缓存.
            main.cache.remove(key, this);
        }

        @SuppressWarnings("all")
        private Mono<T> tryLoad(ContextView contextView) {
            Mono<T> loader = LOADER.getAndSet(this, null);
            //直接load
            if (loader != null) {
                Sinks.One<T> async = Sinks.one();
                loader
                    .switchIfEmpty(Mono.fromRunnable(this::loadEmpty))
                    .subscribe(
                        loaded -> {
                            afterLoaded(loaded);
                            async.emitValue((T) LOADED.get(this), Reactors.emitFailureHandler());
                        },
                        err -> {
                            loadError(err);
                            async.emitError(err, Reactors.emitFailureHandler());
                        },
                        () -> {
                            async.emitEmpty(Reactors.emitFailureHandler());
                        },
                        Context.of(DefaultReactiveCacheContainer.class, this)
                    );
                return async.asMono();
            }
            Sinks.One<T> sink = AWAIT.get(this);
            if (sink == null) {
                return Mono.fromSupplier(() -> loaded);
            }
            //等待其他地方load
            return sink.asMono();
        }

        public Mono<T> ref() {
            return Mono
                .deferContextual(ctx -> {
                    if (ctx.getOrEmpty(DefaultReactiveCacheContainer.class).orElse(null) == this) {
                        //避免递归调用
                        log.warn("recursive call reactive cache [{}]", key);
                        return Mono.justOrEmpty(loaded);
                    }
                    return tryLoad(ctx);
                });
        }
    }
}
