package org.jetlinks.core.defaults;

import org.reactivestreams.Subscription;
import reactor.core.CoreSubscriber;
import reactor.core.Scannable;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Operators;
import reactor.util.context.Context;

import javax.annotation.Nonnull;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
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

        default Object currentSnapshot() {
            return getCached();
        }

        @SuppressWarnings("unchecked")
        default T getCached(Object snapshot) {
            return (T) snapshot;
        }

        default boolean isSnapshotValid(V version, Object snapshot) {
            return isValid(version, getCached(snapshot));
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
        private static final int SWITCHING = 1;
        private static final int LOADER = 2;
        private static final int CACHED = 3;
        private static final int DONE = 4;
        private static final int STAGE_MASK = 0b111;

        private static final int REQUESTED = 1 << 3;
        private static final int CANCELLED = 1 << 4;
        private static final int VERSION_SIGNAL_RECEIVED = 1 << 5;
        private static final int LOADER_VALUE_RECEIVED = 1 << 6;
        private static final int VERSION_DEMAND_SENT = 1 << 7;
        private static final int LOADER_DEMAND_SENT = 1 << 8;

        @SuppressWarnings("rawtypes")
        private static final AtomicIntegerFieldUpdater<MetadataSubscription> STATE =
            AtomicIntegerFieldUpdater.newUpdater(MetadataSubscription.class, "state");

        @SuppressWarnings("rawtypes")
        private static final AtomicReferenceFieldUpdater<MetadataSubscription, Subscription> SUBSCRIPTION =
            AtomicReferenceFieldUpdater.newUpdater(
                MetadataSubscription.class,
                Subscription.class,
                "subscription"
            );

        private final CoreSubscriber<? super T> actual;
        private final Loader<V, T> loader;

        private volatile Subscription subscription;
        private volatile int state = VERSION;

        private MetadataSubscription(CoreSubscriber<? super T> actual,
                                     Loader<V, T> loader) {
            this.actual = actual;
            this.loader = loader;
        }

        private void setSubscription(Subscription next, int expectedStage) {
            for (; ; ) {
                int currentState = state;
                if (isCancelled(currentState) || stage(currentState) != expectedStage) {
                    next.cancel();
                    return;
                }
                Subscription current = subscription;
                if (current != null) {
                    next.cancel();
                    Operators.reportSubscriptionSet();
                    return;
                }
                if (!SUBSCRIPTION.compareAndSet(this, null, next)) {
                    continue;
                }
                currentState = state;
                if (isCancelled(currentState) || stage(currentState) != expectedStage) {
                    if (SUBSCRIPTION.compareAndSet(this, next, null)) {
                        next.cancel();
                    }
                    return;
                }
                requestActive(next, expectedStage);
                return;
            }
        }

        private void versionNext(Object sourceValue) {
            if (!markOnce(VERSION, VERSION_SIGNAL_RECEIVED)) {
                Operators.onNextDropped(sourceValue, actual.currentContext());
                return;
            }
            V version;
            T cached;
            Object snapshot;
            try {
                try {
                    version = loader.convertVersion(sourceValue);
                } catch (IllegalArgumentException | ClassCastException ignore) {
                    version = null;
                }
                if (version == null) {
                    switchTo(Objects.requireNonNull(loader.loadEmpty(), "The empty loader returned a null Publisher"));
                    return;
                }
                snapshot = loader.currentSnapshot();
                cached = loader.getCached(snapshot);
                if (cached != null
                    && loader.isSnapshotValid(version, snapshot)
                    && snapshot == loader.currentSnapshot()) {
                    completeCached(cached);
                    return;
                }
                switchTo(Objects.requireNonNull(loader.load(version), "The loader returned a null Publisher"));
            } catch (Throwable error) {
                fail(Operators.onOperatorError(subscription, error, sourceValue, actual.currentContext()), VERSION);
            }
        }

        private void loaderNext(T metadata) {
            if (!markOnce(LOADER, LOADER_VALUE_RECEIVED)) {
                Operators.onDiscard(metadata, actual.currentContext());
                return;
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
            if (!markOnce(VERSION, VERSION_SIGNAL_RECEIVED)) {
                return;
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
            for (; ; ) {
                int currentState = state;
                if ((currentState & REQUESTED) != 0
                    || isCancelled(currentState)
                    || stage(currentState) == DONE) {
                    return;
                }
                if (!STATE.compareAndSet(this, currentState, currentState | REQUESTED)) {
                    continue;
                }
                Subscription current = subscription;
                int currentStage = stage(state);
                if (current != null && (currentStage == VERSION || currentStage == LOADER)) {
                    requestActive(current, currentStage);
                }
                return;
            }
        }

        @Override
        public void cancel() {
            for (; ; ) {
                int currentState = state;
                if (isCancelled(currentState) || stage(currentState) == DONE) {
                    return;
                }
                if (!STATE.compareAndSet(this, currentState, currentState | CANCELLED)) {
                    continue;
                }
                Subscription current = SUBSCRIPTION.getAndSet(this, null);
                if (current != null) {
                    current.cancel();
                }
                return;
            }
        }

        private void switchTo(Mono<T> next) {
            if (!transitionStage(VERSION, SWITCHING)) {
                return;
            }
            SUBSCRIPTION.set(this, null);
            if (!transitionStage(SWITCHING, LOADER)) {
                return;
            }
            try {
                next.subscribe(new LoaderSubscriber<>(this));
            } catch (Throwable error) {
                fail(Operators.onOperatorError(error, actual.currentContext()), LOADER);
            }
        }

        private void completeCached(T cached) {
            if (!transitionStage(VERSION, CACHED)) {
                Operators.onDiscard(cached, actual.currentContext());
                return;
            }
            SUBSCRIPTION.set(this, null);
            actual.onNext(cached);
            if (transitionStage(CACHED, DONE)) {
                actual.onComplete();
            }
        }

        private void complete(int expectedStage) {
            if (!transitionStage(expectedStage, DONE)) {
                return;
            }
            SUBSCRIPTION.set(this, null);
            actual.onComplete();
        }

        private void fail(Throwable error, int expectedStage) {
            if (!transitionStage(expectedStage, DONE)) {
                Operators.onErrorDropped(error, actual.currentContext());
                return;
            }
            Subscription current = SUBSCRIPTION.getAndSet(this, null);
            if (current != null) {
                current.cancel();
            }
            actual.onError(error);
        }

        private boolean markOnce(int expectedStage, int flag) {
            for (; ; ) {
                int currentState = state;
                if (isCancelled(currentState)
                    || stage(currentState) != expectedStage
                    || (currentState & flag) != 0) {
                    return false;
                }
                if (STATE.compareAndSet(this, currentState, currentState | flag)) {
                    return true;
                }
            }
        }

        private boolean transitionStage(int expectedStage, int nextStage) {
            for (; ; ) {
                int currentState = state;
                if (isCancelled(currentState) || stage(currentState) != expectedStage) {
                    return false;
                }
                int nextState = (currentState & ~STAGE_MASK) | nextStage;
                if (STATE.compareAndSet(this, currentState, nextState)) {
                    return true;
                }
            }
        }

        private void requestActive(Subscription current, int expectedStage) {
            int demandFlag = expectedStage == VERSION
                ? VERSION_DEMAND_SENT
                : LOADER_DEMAND_SENT;
            for (; ; ) {
                int currentState = state;
                if (isCancelled(currentState)
                    || stage(currentState) != expectedStage
                    || (currentState & REQUESTED) == 0
                    || (currentState & demandFlag) != 0
                    || subscription != current) {
                    return;
                }
                if (STATE.compareAndSet(this, currentState, currentState | demandFlag)) {
                    current.request(Long.MAX_VALUE);
                    return;
                }
            }
        }

        private static int stage(int state) {
            return state & STAGE_MASK;
        }

        private static boolean isCancelled(int state) {
            return (state & CANCELLED) != 0;
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
                return isCancelled(state);
            }
            if (attribute == Attr.TERMINATED) {
                return stage(state) == DONE;
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
