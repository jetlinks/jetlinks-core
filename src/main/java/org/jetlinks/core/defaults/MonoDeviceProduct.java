package org.jetlinks.core.defaults;

import org.jetlinks.core.Values;
import org.jetlinks.core.config.ConfigStorage;
import org.jetlinks.core.device.DeviceProductOperator;
import org.jetlinks.core.device.DeviceRegistry;
import org.reactivestreams.Subscription;
import reactor.core.CoreSubscriber;
import reactor.core.Scannable;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Operators;
import reactor.util.context.Context;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

/**
 * 在同步配置热路径中直接解析设备产品，异步来源继续使用原 Reactor 链。
 * 每次订阅重新读取产品标识和版本，不保存产品或配置结果。
 */
final class MonoDeviceProduct extends Mono<DeviceProductOperator> implements Scannable {

    private final Mono<ConfigStorage> source;
    private final DeviceRegistry registry;
    private final Collection<String> keys;
    private final String productIdKey;
    private final String productVersionKey;

    private MonoDeviceProduct(Mono<ConfigStorage> source,
                              DeviceRegistry registry,
                              Collection<String> keys,
                              String productIdKey,
                              String productVersionKey) {
        this.source = source;
        this.registry = registry;
        this.keys = keys;
        this.productIdKey = productIdKey;
        this.productVersionKey = productVersionKey;
    }

    static Mono<DeviceProductOperator> create(Mono<ConfigStorage> source,
                                              DeviceRegistry registry,
                                              Collection<String> keys,
                                              String productIdKey,
                                              String productVersionKey) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(registry, "registry");
        Objects.requireNonNull(keys, "keys");
        Objects.requireNonNull(productIdKey, "productIdKey");
        Objects.requireNonNull(productVersionKey, "productVersionKey");
        if (!(source instanceof Callable)) {
            return source
                .flatMap(storage -> storage.getConfigs(keys))
                .flatMap(values -> readProduct(registry, values, productIdKey, productVersionKey));
        }
        return onAssembly(new MonoDeviceProduct(source, registry, keys, productIdKey, productVersionKey));
    }

    @Override
    public void subscribe(@Nonnull CoreSubscriber<? super DeviceProductOperator> actual) {
        actual.onSubscribe(new ProductSubscription(actual, this));
    }

    private void resolve(ProductSubscription subscription) {
        ConfigStorage storage;
        try {
            storage = ConfigStorage.class.cast(((Callable<?>) source).call());
        } catch (Throwable error) {
            subscription.fail(Operators.onOperatorError(error, subscription.currentContext()));
            return;
        }
        if (storage == null) {
            subscription.complete();
            return;
        }

        Mono<Values> valuesSource;
        try {
            valuesSource = Objects.requireNonNull(storage.getConfigs(keys), "The mapper returned a null Publisher");
        } catch (Throwable error) {
            subscription.fail(Operators.onOperatorError(null, error, storage, subscription.currentContext()));
            return;
        }
        if (!(valuesSource instanceof Callable)) {
            try {
                valuesSource
                    .flatMap(this::readProduct)
                    .subscribe(new ProductSubscriber(subscription));
            } catch (Throwable error) {
                subscription.fail(Operators.onOperatorError(error, subscription.currentContext()));
            }
            return;
        }

        Values values;
        try {
            values = Values.class.cast(((Callable<?>) valuesSource).call());
        } catch (Throwable error) {
            subscription.fail(Operators.onOperatorError(null, error, storage, subscription.currentContext()));
            return;
        }
        if (values == null) {
            subscription.complete();
            return;
        }

        Mono<DeviceProductOperator> productSource;
        try {
            productSource = Objects.requireNonNull(readProduct(values), "The mapper returned a null Publisher");
        } catch (Throwable error) {
            subscription.fail(Operators.onOperatorError(null, error, values, subscription.currentContext()));
            return;
        }
        if (!(productSource instanceof Callable)) {
            try {
                productSource.subscribe(new ProductSubscriber(subscription));
            } catch (Throwable error) {
                subscription.fail(Operators.onOperatorError(error, subscription.currentContext()));
            }
            return;
        }

        DeviceProductOperator product;
        try {
            product = DeviceProductOperator.class.cast(((Callable<?>) productSource).call());
        } catch (Throwable error) {
            subscription.fail(Operators.onOperatorError(null, error, values, subscription.currentContext()));
            return;
        }
        if (product == null) {
            subscription.complete();
        } else {
            subscription.complete(product);
        }
    }

    private Mono<DeviceProductOperator> readProduct(Values values) {
        return readProduct(registry, values, productIdKey, productVersionKey);
    }

    private static Mono<DeviceProductOperator> readProduct(DeviceRegistry registry,
                                                           Values values,
                                                           String productIdKey,
                                                           String productVersionKey) {
        String productId = values.getString(productIdKey, (String) null);
        String version = values.getString(productVersionKey, (String) null);
        return productId == null || productId.trim().isEmpty() ? Mono.empty() : registry.getProduct(productId, version);
    }

    Mono<DeviceProductOperator> lookup(Context context) {
        ConfigStorage storage;
        try {
            storage = ConfigStorage.class.cast(((Callable<?>) source).call());
        } catch (Throwable error) {
            return Mono.error(Operators.onOperatorError(error, context));
        }
        if (storage == null) {
            return Mono.empty();
        }

        Mono<Values> valuesSource;
        try {
            valuesSource = Objects.requireNonNull(storage.getConfigs(keys), "The mapper returned a null Publisher");
        } catch (Throwable error) {
            return Mono.error(Operators.onOperatorError(null, error, storage, context));
        }
        if (!(valuesSource instanceof Callable)) {
            return valuesSource.flatMap(this::readProduct);
        }

        Values values;
        try {
            values = Values.class.cast(((Callable<?>) valuesSource).call());
        } catch (Throwable error) {
            return Mono.error(Operators.onOperatorError(null, error, storage, context));
        }
        if (values == null) {
            return Mono.empty();
        }

        try {
            return Objects.requireNonNull(readProduct(values), "The mapper returned a null Publisher");
        } catch (Throwable error) {
            return Mono.error(Operators.onOperatorError(null, error, values, context));
        }
    }

    @Override
    public Object scanUnsafe(@Nonnull Attr attribute) {
        if (attribute == Attr.PARENT) {
            return source;
        }
        if (attribute == Attr.RUN_STYLE) {
            return Attr.RunStyle.SYNC;
        }
        return null;
    }

    private static final class ProductSubscription implements Subscription, Scannable {

        private static final int READY = 0;
        private static final int ACTIVE = 1;
        private static final int EMITTING = 2;
        private static final int DONE = 3;
        private static final int STAGE_MASK = 0b11;

        private static final int CANCELLED = 1 << 2;
        private static final int VALUE_RECEIVED = 1 << 3;

        @SuppressWarnings("rawtypes")
        private static final AtomicIntegerFieldUpdater<ProductSubscription> STATE =
            AtomicIntegerFieldUpdater.newUpdater(ProductSubscription.class, "state");

        @SuppressWarnings("rawtypes")
        private static final AtomicReferenceFieldUpdater<ProductSubscription, Subscription> SUBSCRIPTION =
            AtomicReferenceFieldUpdater.newUpdater(
                ProductSubscription.class,
                Subscription.class,
                "subscription"
            );

        private final CoreSubscriber<? super DeviceProductOperator> actual;
        private final MonoDeviceProduct owner;

        private volatile Subscription subscription;
        private volatile int state = READY;

        private ProductSubscription(CoreSubscriber<? super DeviceProductOperator> actual,
                                    MonoDeviceProduct owner) {
            this.actual = actual;
            this.owner = owner;
        }

        @Override
        public void request(long count) {
            if (!Operators.validate(count)) {
                return;
            }
            if (transitionStage(READY, ACTIVE)) {
                owner.resolve(this);
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

        private void setSubscription(Subscription next) {
            for (; ; ) {
                int currentState = state;
                if (isCancelled(currentState) || stage(currentState) != ACTIVE) {
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
                if (isCancelled(currentState) || stage(currentState) != ACTIVE) {
                    if (SUBSCRIPTION.compareAndSet(this, next, null)) {
                        next.cancel();
                    }
                    return;
                }
                next.request(Long.MAX_VALUE);
                return;
            }
        }

        private void next(DeviceProductOperator product) {
            if (!markValue()) {
                Operators.onDiscard(product, currentContext());
                return;
            }
            actual.onNext(product);
        }

        private void complete(DeviceProductOperator product) {
            if (!transitionStage(ACTIVE, EMITTING)) {
                Operators.onDiscard(product, currentContext());
                return;
            }
            actual.onNext(product);
            if (transitionStage(EMITTING, DONE)) {
                SUBSCRIPTION.set(this, null);
                actual.onComplete();
            }
        }

        private void complete() {
            if (transitionStage(ACTIVE, DONE)) {
                SUBSCRIPTION.set(this, null);
                actual.onComplete();
            }
        }

        private void fail(Throwable error) {
            if (!transitionStage(ACTIVE, DONE)) {
                Operators.onErrorDropped(error, currentContext());
                return;
            }
            Subscription current = SUBSCRIPTION.getAndSet(this, null);
            if (current != null) {
                current.cancel();
            }
            actual.onError(error);
        }

        private boolean markValue() {
            for (; ; ) {
                int currentState = state;
                if (isCancelled(currentState)
                    || stage(currentState) != ACTIVE
                    || (currentState & VALUE_RECEIVED) != 0) {
                    return false;
                }
                if (STATE.compareAndSet(this, currentState, currentState | VALUE_RECEIVED)) {
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

        private static int stage(int state) {
            return state & STAGE_MASK;
        }

        private static boolean isCancelled(int state) {
            return (state & CANCELLED) != 0;
        }

        private Context currentContext() {
            return actual.currentContext();
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

    private static final class ProductSubscriber implements CoreSubscriber<DeviceProductOperator> {

        private final ProductSubscription subscription;

        private ProductSubscriber(ProductSubscription subscription) {
            this.subscription = subscription;
        }

        @Override
        public void onSubscribe(@Nonnull Subscription source) {
            subscription.setSubscription(source);
        }

        @Override
        public void onNext(DeviceProductOperator product) {
            subscription.next(product);
        }

        @Override
        public void onError(Throwable error) {
            subscription.fail(error);
        }

        @Override
        public void onComplete() {
            subscription.complete();
        }

        @Override
        @Nonnull
        public Context currentContext() {
            return subscription.currentContext();
        }
    }
}
