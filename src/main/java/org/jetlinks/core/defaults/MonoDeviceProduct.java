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
            valuesSource
                .flatMap(this::readProduct)
                .subscribe(new ProductSubscriber(subscription));
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
            productSource.subscribe(new ProductSubscriber(subscription));
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

        private final CoreSubscriber<? super DeviceProductOperator> actual;
        private final MonoDeviceProduct owner;

        private Subscription subscription;
        private boolean started;
        private boolean cancelled;
        private boolean done;

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
            synchronized (this) {
                if (started || cancelled || done) {
                    return;
                }
                started = true;
            }
            owner.resolve(this);
        }

        @Override
        public void cancel() {
            Subscription current;
            synchronized (this) {
                if (cancelled || done) {
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

        private void setSubscription(Subscription next) {
            synchronized (this) {
                if (cancelled || done) {
                    next.cancel();
                    return;
                }
                if (subscription != null) {
                    next.cancel();
                    Operators.reportSubscriptionSet();
                    return;
                }
                subscription = next;
            }
            next.request(Long.MAX_VALUE);
        }

        private void next(DeviceProductOperator product) {
            synchronized (this) {
                if (cancelled || done) {
                    Operators.onDiscard(product, currentContext());
                    return;
                }
            }
            actual.onNext(product);
        }

        private void complete(DeviceProductOperator product) {
            synchronized (this) {
                if (cancelled || done) {
                    Operators.onDiscard(product, currentContext());
                    return;
                }
            }
            actual.onNext(product);
            synchronized (this) {
                if (cancelled || done) {
                    return;
                }
                done = true;
                subscription = null;
            }
            actual.onComplete();
        }

        private void complete() {
            synchronized (this) {
                if (cancelled || done) {
                    return;
                }
                done = true;
                subscription = null;
            }
            actual.onComplete();
        }

        private void fail(Throwable error) {
            Subscription current;
            synchronized (this) {
                if (cancelled || done) {
                    Operators.onErrorDropped(error, currentContext());
                    return;
                }
                done = true;
                current = subscription;
                subscription = null;
            }
            if (current != null) {
                current.cancel();
            }
            actual.onError(error);
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
                return cancelled;
            }
            if (attribute == Attr.TERMINATED) {
                return done;
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
