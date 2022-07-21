package org.jetlinks.core.cache;

import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.util.context.ContextView;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.function.BiFunction;

class DefaultReactiveCacheContainer<T> implements ReactiveCacheContainer<T> {

    private final Map<String, Container<T>> cache = new ConcurrentHashMap<>();

    @Override
    public Mono<T> compute(String key, BiFunction<String, T, Mono<T>> compute) {

        return cache
                .compute(key, (k, old) -> {
                    if (old == null) {
                        Mono<T> loader = compute.apply(key, null);
                        return new Container<>(
                                key,
                                DefaultReactiveCacheContainer.this,
                                loader);
                    }
                    old.update(compute.apply(key, old.loaded));
                    return old;
                })
                .ref();
    }

    @Override
    public Mono<T> get(String key, Mono<T> defaultValue) {
        Container<T> container = cache.get(key);
        if (container != null) {
            return container.ref();
        }
        return defaultValue;
    }

    @Override
    public T getNow(String key) {
        Container<T> container = cache.get(key);
        if (container != null) {
            return container.loaded;
        }
        return null;
    }

    @Override
    public T remove(String key) {
        Container<T> container = cache.remove(key);
        if (null != container) {
            container.dispose();
        }
        return container == null ? null : container.loaded;
    }


    @SuppressWarnings("rawtypes")
    private final static AtomicReferenceFieldUpdater<Container, Mono> LOADER
            = AtomicReferenceFieldUpdater.newUpdater(Container.class, Mono.class, "loader");

    @Override
    public void dispose() {
        cache.values().forEach(Container::dispose);
        cache.clear();
    }


    static class Container<T> implements Disposable {
        private final DefaultReactiveCacheContainer<T> main;
        private final String key;
        private Sinks.One<T> await;
        public volatile T loaded;
        protected volatile Mono<T> loader;
        private volatile Disposable disposable;

        public Container(String key, DefaultReactiveCacheContainer<T> main, Mono<T> loader) {
            this.key = key;
            this.main = main;
            this.loader = loader;
            update(loader);
        }

        public void update(Mono<T> ref) {
            if (disposable != null && !disposable.isDisposed()) {
                disposable.dispose();
            }
            Sinks.One<T> old = this.await;
            this.await = Sinks.one();
            if (old != null && old.currentSubscriberCount() > 0) {
                old.tryEmitEmpty();
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
            this.await.tryEmitValue(data);
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

            await.tryEmitError(err);
            main.remove(key);
        }

        private void loadEmpty() {
            await.tryEmitEmpty();
        }

        private void tryLoad(ContextView contextView) {
            @SuppressWarnings("all")
            Mono<T> loader = LOADER.getAndSet(this, null);

            if (loader != null) {
                disposable = loader
                        .contextWrite(contextView)
                        .subscribe();
            }
        }

        public Mono<T> ref() {
            return Mono.deferContextual(ctx -> {
                tryLoad(ctx);
                return await.asMono();
            });
        }
    }
}
