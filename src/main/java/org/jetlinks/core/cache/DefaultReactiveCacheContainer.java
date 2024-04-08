package org.jetlinks.core.cache;

import lombok.extern.slf4j.Slf4j;
import org.jetlinks.core.utils.Reactors;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.util.context.Context;
import reactor.util.context.ContextView;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
class DefaultReactiveCacheContainer<K, V> implements ReactiveCacheContainer<K, V> {

    private final Map<K, Container<K, V>> cache = new ConcurrentHashMap<>();

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
                old.update(compute.apply(k, old.loaded));
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

        private final DefaultReactiveCacheContainer<K, T> main;
        private final K key;
        private Sinks.One<T> await;
        public volatile T loaded;
        protected volatile Mono<T> loader;
        private volatile Disposable disposable;

        public Container(K key, DefaultReactiveCacheContainer<K, T> main, Mono<T> loader) {
            this.key = key;
            this.main = main;
            this.loader = loader;
            update(loader);
        }

        public Container(K key, DefaultReactiveCacheContainer<K, T> main, T loaded) {
            this.key = key;
            this.main = main;
            this.loaded = loaded;
            this.loader = Mono.just(loaded);
            update(this.loader);
        }

        public void update(Mono<T> ref) {
            if (disposable != null && !disposable.isDisposed()) {
                disposable.dispose();
            }
            Sinks.One<T> old = this.await;
            this.await = Sinks.one();
            if (old != null && old.currentSubscriberCount() > 0) {
                old.emitEmpty(Reactors.emitFailureHandler());
            }
            loader = ref
                .switchIfEmpty(Mono.fromRunnable(this::loadEmpty))
                .doOnError(this::loadError)
                .doOnNext(this::afterLoaded);
        }

        private void afterLoaded(T data) {
            if (data != loaded && loaded instanceof Disposable) {
                ((Disposable) loaded).dispose();
            }
            loaded = data;
            this.await.emitValue(data, Reactors.emitFailureHandler());
        }


        public void dispose() {
            if (disposable != null && !disposable.isDisposed()) {
                disposable.dispose();
            }
            T loaded = this.loaded;
            if (loaded instanceof Disposable) {
                ((Disposable) loaded).dispose();
            }
        }

        private void loadError(Throwable err) {

            await.emitError(err, Reactors.emitFailureHandler());
            main.remove(key);
        }

        private void loadEmpty() {
            await.emitEmpty(Reactors.emitFailureHandler());
        }

        private void tryLoad(ContextView contextView) {
            @SuppressWarnings("all")
            Mono<T> loader = LOADER.getAndSet(this, null);

            if (loader != null) {
                disposable = loader
                    .contextWrite(Context.of(contextView).put(DefaultReactiveCacheContainer.class, this))
                    .subscribe();
            }
        }

        public Mono<T> ref() {
            return Mono
                .deferContextual(ctx -> {
                    if (ctx.getOrEmpty(DefaultReactiveCacheContainer.class).orElse(null) == this) {
                        //避免递归调用
                        log.warn("recursive call reactive cache [{}]", key);
                        return Mono.justOrEmpty(loaded);
                    }
                    tryLoad(ctx);
                    return await.asMono();
                });
        }
    }
}
