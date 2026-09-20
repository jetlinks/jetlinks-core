package org.jetlinks.core.defaults;

import org.reactivestreams.Subscription;
import reactor.core.CoreSubscriber;
import reactor.core.Scannable;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Operators;
import reactor.util.context.Context;

import javax.annotation.Nonnull;
import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * 根据版本返回当前物模型，版本未命中时切换到原加载流程。
 * 每次订阅独立检查版本，不保存加载结果或跨订阅状态。
 */
final class MonoVersionedMetadata<V, T> extends Mono<T> implements Scannable {

    private final Mono<?> versionSource;
    private final Loader<V, T> loader;

    private MonoVersionedMetadata(Mono<?> versionSource,
                                  Loader<V, T> loader) {
        this.versionSource = Objects.requireNonNull(versionSource, "versionSource");
        this.loader = Objects.requireNonNull(loader, "loader");
    }

    static <V, T> Mono<T> create(Mono<?> versionSource,
                                 Loader<V, T> loader) {
        return onAssembly(new MonoVersionedMetadata<>(versionSource, loader));
    }

    static <V, T> Mono<T> create(Mono<V> versionSource,
                                 Supplier<T> cachedSupplier,
                                 BiPredicate<V, T> cacheValidator,
                                 Function<V, Mono<T>> loader,
                                 Supplier<Mono<T>> emptyLoader) {
        return create(versionSource, new Loader<V, T>() {
            @Override
            public T getCached() {
                return cachedSupplier.get();
            }

            @Override
            public boolean isValid(V version, T cached) {
                return cacheValidator.test(version, cached);
            }

            @Override
            public Mono<T> load(V version) {
                return loader.apply(version);
            }

            @Override
            public Mono<T> loadEmpty() {
                return emptyLoader.get();
            }
        });
    }

    @Override
    @SuppressWarnings("unchecked")
    public void subscribe(@Nonnull CoreSubscriber<? super T> actual) {
        MetadataSubscription<V, T> subscription = new MetadataSubscription<>(actual, loader);
        actual.onSubscribe(subscription);
        try {
            ((Mono<Object>) versionSource).subscribe(new VersionSubscriber<>(subscription));
        } catch (Throwable error) {
            subscription.versionError(Operators.onOperatorError(error, actual.currentContext()));
        }
    }

    @Override
    public Object scanUnsafe(@Nonnull Attr attribute) {
        if (attribute == Attr.PARENT) {
            return versionSource;
        }
        return null;
    }

    interface Loader<V, T> {

        @SuppressWarnings("unchecked")
        default V convertVersion(Object value) {
            return (V) value;
        }

        T getCached();

        boolean isValid(V version, T cached);

        Mono<T> load(V version);

        default Mono<T> loadEmpty() {
            return Mono.empty();
        }
    }

    private static final class MetadataSubscription<V, T> implements Subscription, Scannable {

        private static final int VERSION = 0;
        private static final int LOADER = 1;
        private static final int DONE = 2;

        private final CoreSubscriber<? super T> actual;
        private final Loader<V, T> loader;

        private Subscription subscription;
        private boolean requested;
        private boolean cancelled;
        private boolean versionReceived;
        private int stage = VERSION;

        private MetadataSubscription(CoreSubscriber<? super T> actual,
                                     Loader<V, T> loader) {
            this.actual = actual;
            this.loader = loader;
        }

        private void setSubscription(Subscription next, int expectedStage) {
            boolean request;
            synchronized (this) {
                if (cancelled || stage != expectedStage) {
                    next.cancel();
                    return;
                }
                if (subscription != null) {
                    next.cancel();
                    Operators.reportSubscriptionSet();
                    return;
                }
                subscription = next;
                request = requested;
            }
            if (request) {
                next.request(Long.MAX_VALUE);
            }
        }

        private void versionNext(Object sourceValue) {
            synchronized (this) {
                if (cancelled || stage != VERSION || versionReceived) {
                    Operators.onNextDropped(sourceValue, actual.currentContext());
                    return;
                }
                versionReceived = true;
            }
            V version;
            T cached;
            try {
                version = loader.convertVersion(sourceValue);
                if (version == null) {
                    switchTo(Objects.requireNonNull(loader.loadEmpty(), "The empty loader returned a null Publisher"));
                    return;
                }
                cached = loader.getCached();
                if (cached != null
                    && loader.isValid(version, cached)
                    && cached == loader.getCached()) {
                    completeCached(cached);
                    return;
                }
                switchTo(Objects.requireNonNull(loader.load(version), "The loader returned a null Publisher"));
            } catch (Throwable error) {
                fail(Operators.onOperatorError(subscription, error, sourceValue, actual.currentContext()), VERSION);
            }
        }

        private void loaderNext(T metadata) {
            synchronized (this) {
                if (cancelled || stage != LOADER) {
                    Operators.onDiscard(metadata, actual.currentContext());
                    return;
                }
            }
            actual.onNext(metadata);
        }

        private void versionError(Throwable error) {
            fail(error, VERSION);
        }

        private void loaderError(Throwable error) {
            fail(error, LOADER);
        }

        private void versionComplete() {
            synchronized (this) {
                if (cancelled || stage != VERSION || versionReceived) {
                    return;
                }
            }
            try {
                switchTo(Objects.requireNonNull(loader.loadEmpty(), "The empty loader returned a null Publisher"));
            } catch (Throwable error) {
                fail(Operators.onOperatorError(subscription, error, actual.currentContext()), VERSION);
            }
        }

        private void loaderComplete() {
            complete(LOADER);
        }

        @Override
        public void request(long count) {
            if (!Operators.validate(count)) {
                return;
            }
            Subscription current;
            synchronized (this) {
                if (requested || cancelled || stage == DONE) {
                    return;
                }
                requested = true;
                current = subscription;
            }
            if (current != null) {
                current.request(Long.MAX_VALUE);
            }
        }

        @Override
        public void cancel() {
            Subscription current;
            synchronized (this) {
                if (cancelled || stage == DONE) {
                    return;
                }
                cancelled = true;
                current = subscription;
                subscription = null;
            }
            if (current != null) {
                current.cancel();
            }
        }

        private void switchTo(Mono<T> next) {
            synchronized (this) {
                if (cancelled || stage != VERSION) {
                    return;
                }
                stage = LOADER;
                subscription = null;
            }
            try {
                next.subscribe(new LoaderSubscriber<>(this));
            } catch (Throwable error) {
                fail(Operators.onOperatorError(error, actual.currentContext()), LOADER);
            }
        }

        private void completeCached(T cached) {
            synchronized (this) {
                if (cancelled || stage != VERSION) {
                    Operators.onDiscard(cached, actual.currentContext());
                    return;
                }
                stage = DONE;
                subscription = null;
            }
            actual.onNext(cached);
            actual.onComplete();
        }

        private void complete(int expectedStage) {
            synchronized (this) {
                if (cancelled || stage != expectedStage) {
                    return;
                }
                stage = DONE;
                subscription = null;
            }
            actual.onComplete();
        }

        private void fail(Throwable error, int expectedStage) {
            Subscription current;
            synchronized (this) {
                if (cancelled || stage != expectedStage) {
                    Operators.onErrorDropped(error, actual.currentContext());
                    return;
                }
                stage = DONE;
                current = subscription;
                subscription = null;
            }
            if (current != null) {
                current.cancel();
            }
            actual.onError(error);
        }

        @Override
        public Object scanUnsafe(@Nonnull Attr attribute) {
            if (attribute == Attr.ACTUAL) {
                return actual;
            }
            if (attribute == Attr.PARENT) {
                return subscription;
            }
            if (attribute == Attr.CANCELLED) {
                return cancelled;
            }
            if (attribute == Attr.TERMINATED) {
                return stage == DONE;
            }
            if (attribute == Attr.RUN_STYLE) {
                return Attr.RunStyle.SYNC;
            }
            return null;
        }
    }

    private static final class VersionSubscriber<V> implements CoreSubscriber<Object> {

        private final MetadataSubscription<V, ?> subscription;

        private VersionSubscriber(MetadataSubscription<V, ?> subscription) {
            this.subscription = subscription;
        }

        @Override
        public void onSubscribe(@Nonnull Subscription source) {
            subscription.setSubscription(source, MetadataSubscription.VERSION);
        }

        @Override
        public void onNext(Object version) {
            subscription.versionNext(version);
        }

        @Override
        public void onError(Throwable error) {
            subscription.versionError(error);
        }

        @Override
        public void onComplete() {
            subscription.versionComplete();
        }

        @Override
        @Nonnull
        public Context currentContext() {
            return subscription.actual.currentContext();
        }
    }

    private static final class LoaderSubscriber<T> implements CoreSubscriber<T> {

        private final MetadataSubscription<?, T> subscription;

        private LoaderSubscriber(MetadataSubscription<?, T> subscription) {
            this.subscription = subscription;
        }

        @Override
        public void onSubscribe(@Nonnull Subscription source) {
            subscription.setSubscription(source, MetadataSubscription.LOADER);
        }

        @Override
        public void onNext(T metadata) {
            subscription.loaderNext(metadata);
        }

        @Override
        public void onError(Throwable error) {
            subscription.loaderError(error);
        }

        @Override
        public void onComplete() {
            subscription.loaderComplete();
        }

        @Override
        @Nonnull
        public Context currentContext() {
            return subscription.actual.currentContext();
        }
    }
}
