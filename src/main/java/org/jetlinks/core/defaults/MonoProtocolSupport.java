package org.jetlinks.core.defaults;

import org.jetlinks.core.ProtocolSupport;
import org.jetlinks.core.ProtocolSupports;
import org.jetlinks.core.Value;
import org.jetlinks.core.config.ConfigStorage;
import org.jetlinks.core.device.DeviceProductOperator;
import org.reactivestreams.Subscription;
import reactor.core.CoreSubscriber;
import reactor.core.Scannable;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Operators;
import reactor.util.context.Context;

import javax.annotation.Nonnull;
import java.util.Objects;
import java.util.concurrent.Callable;

/**
 * 在同步配置热路径中直接解析协议，设备自身未配置或协议源为空时再读取产品协议。
 * 每次订阅重新读取当前配置，不保存配置值、产品或协议结果。
 */
final class MonoProtocolSupport extends Mono<ProtocolSupport> implements Scannable {

    private static final int STAGE_STORAGE = 1;
    private static final int STAGE_CONFIG = 2;
    private static final int STAGE_SUPPORT = 3;
    private static final int STAGE_PRODUCT = 4;
    private static final int STAGE_PRODUCT_SUPPORT = 5;

    private final Mono<ConfigStorage> source;
    private final ProtocolSupports supports;
    private final String protocolKey;
    private final Mono<? extends DeviceProductOperator> fallbackProduct;

    private MonoProtocolSupport(Mono<ConfigStorage> source,
                                ProtocolSupports supports,
                                String protocolKey,
                                Mono<? extends DeviceProductOperator> fallbackProduct) {
        this.source = Objects.requireNonNull(source, "source");
        this.supports = Objects.requireNonNull(supports, "supports");
        this.protocolKey = Objects.requireNonNull(protocolKey, "protocolKey");
        this.fallbackProduct = fallbackProduct;
    }

    static Mono<ProtocolSupport> create(Mono<ConfigStorage> source,
                                        ProtocolSupports supports,
                                        String protocolKey,
                                        Mono<? extends DeviceProductOperator> fallbackProduct) {
        return onAssembly(new MonoProtocolSupport(source, supports, protocolKey, fallbackProduct));
    }

    @Override
    public void subscribe(@Nonnull CoreSubscriber<? super ProtocolSupport> actual) {
        actual.onSubscribe(new ProtocolSubscription(actual, this));
    }

    private void resolve(ProtocolSubscription subscription) {
        if (!(source instanceof Callable)) {
            subscribeStage(source, STAGE_STORAGE, subscription);
            return;
        }
        ConfigStorage storage;
        try {
            storage = ConfigStorage.class.cast(((Callable<?>) source).call());
        } catch (Throwable error) {
            subscription.fail(Operators.onOperatorError(error, subscription.currentContext()));
            return;
        }
        if (storage == null) {
            resolveFallback(subscription);
        } else {
            resolveStorage(storage, subscription);
        }
    }

    private void resolveStorage(ConfigStorage storage, ProtocolSubscription subscription) {
        Mono<Value> configSource;
        try {
            configSource = Objects.requireNonNull(storage.getConfig(protocolKey), "The mapper returned a null Publisher");
        } catch (Throwable error) {
            subscription.fail(Operators.onOperatorError(null, error, storage, subscription.currentContext()));
            return;
        }
        if (!(configSource instanceof Callable)) {
            subscribeStage(configSource, STAGE_CONFIG, subscription);
            return;
        }
        Value value;
        try {
            value = Value.class.cast(((Callable<?>) configSource).call());
        } catch (Throwable error) {
            subscription.fail(Operators.onOperatorError(null, error, storage, subscription.currentContext()));
            return;
        }
        if (value == null) {
            resolveFallback(subscription);
        } else {
            resolveProtocol(value, subscription);
        }
    }

    private void resolveProtocol(Value value, ProtocolSubscription subscription) {
        String protocol;
        try {
            protocol = value.as(String.class);
        } catch (Throwable error) {
            subscription.fail(Operators.onOperatorError(null, error, value, subscription.currentContext()));
            return;
        }
        if (protocol == null) {
            resolveFallback(subscription);
            return;
        }

        Mono<ProtocolSupport> supportSource;
        try {
            supportSource = Objects.requireNonNull(supports.getProtocol(protocol), "The mapper returned a null Publisher");
        } catch (Throwable error) {
            subscription.fail(Operators.onOperatorError(null, error, protocol, subscription.currentContext()));
            return;
        }
        resolveSupport(supportSource, STAGE_SUPPORT, true, subscription);
    }

    private void resolveSupport(Mono<ProtocolSupport> supportSource,
                                int stage,
                                boolean fallbackIfEmpty,
                                ProtocolSubscription subscription) {
        if (!(supportSource instanceof Callable)) {
            subscribeStage(supportSource, stage, subscription);
            return;
        }
        ProtocolSupport support;
        try {
            support = ProtocolSupport.class.cast(((Callable<?>) supportSource).call());
        } catch (Throwable error) {
            subscription.fail(Operators.onOperatorError(error, subscription.currentContext()));
            return;
        }
        if (support != null) {
            subscription.complete(support);
        } else if (fallbackIfEmpty) {
            resolveFallback(subscription);
        } else {
            subscription.complete();
        }
    }

    private void resolveFallback(ProtocolSubscription subscription) {
        if (fallbackProduct == null) {
            subscription.complete();
            return;
        }
        Mono<? extends DeviceProductOperator> productSource = fallbackProduct;
        if (productSource instanceof MonoDeviceProduct) {
            productSource = ((MonoDeviceProduct) productSource).lookup(subscription.currentContext());
        }
        if (!(productSource instanceof Callable)) {
            subscribeStage(productSource, STAGE_PRODUCT, subscription);
            return;
        }
        DeviceProductOperator product;
        try {
            product = DeviceProductOperator.class.cast(((Callable<?>) productSource).call());
        } catch (Throwable error) {
            subscription.fail(Operators.onOperatorError(error, subscription.currentContext()));
            return;
        }
        if (product == null) {
            subscription.complete();
        } else {
            resolveProduct(product, subscription);
        }
    }

    private void resolveProduct(DeviceProductOperator product, ProtocolSubscription subscription) {
        Mono<ProtocolSupport> supportSource;
        try {
            supportSource = Objects.requireNonNull(product.getProtocol(), "The mapper returned a null Publisher");
        } catch (Throwable error) {
            subscription.fail(Operators.onOperatorError(null, error, product, subscription.currentContext()));
            return;
        }
        resolveSupport(supportSource, STAGE_PRODUCT_SUPPORT, false, subscription);
    }

    private void subscribeStage(Mono<?> stageSource, int stage, ProtocolSubscription subscription) {
        stageSource.subscribe(new StageSubscriber(subscription, stage));
    }

    private void stageValue(int stage, Object value, ProtocolSubscription subscription) {
        switch (stage) {
            case STAGE_STORAGE:
                resolveStorage((ConfigStorage) value, subscription);
                break;
            case STAGE_CONFIG:
                resolveProtocol((Value) value, subscription);
                break;
            case STAGE_SUPPORT:
            case STAGE_PRODUCT_SUPPORT:
                subscription.complete((ProtocolSupport) value);
                break;
            case STAGE_PRODUCT:
                resolveProduct((DeviceProductOperator) value, subscription);
                break;
            default:
                subscription.fail(new IllegalStateException("unknown protocol stage: " + stage));
                break;
        }
    }

    private void stageEmpty(int stage, ProtocolSubscription subscription) {
        if (stage == STAGE_STORAGE || stage == STAGE_CONFIG || stage == STAGE_SUPPORT) {
            resolveFallback(subscription);
        } else {
            subscription.complete();
        }
    }

    @Override
    public Object scanUnsafe(@Nonnull Attr attribute) {
        if (attribute == Attr.PARENT) {
            return source;
        }
        return null;
    }

    private static final class ProtocolSubscription implements Subscription, Scannable {

        private final CoreSubscriber<? super ProtocolSupport> actual;
        private final MonoProtocolSupport owner;

        private Subscription subscription;
        private boolean started;
        private boolean cancelled;
        private boolean done;

        private ProtocolSubscription(CoreSubscriber<? super ProtocolSupport> actual,
                                     MonoProtocolSupport owner) {
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
                subscription = next;
            }
            next.request(Long.MAX_VALUE);
        }

        private void complete(ProtocolSupport support) {
            synchronized (this) {
                if (cancelled || done) {
                    Operators.onDiscard(support, currentContext());
                    return;
                }
            }
            actual.onNext(support);
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
            if (attribute == Attr.CANCELLED) {
                return cancelled;
            }
            if (attribute == Attr.TERMINATED) {
                return done;
            }
            return null;
        }
    }

    private static final class StageSubscriber implements CoreSubscriber<Object> {

        private final ProtocolSubscription parent;
        private final int stage;

        private boolean valueReceived;

        private StageSubscriber(ProtocolSubscription parent, int stage) {
            this.parent = parent;
            this.stage = stage;
        }

        @Override
        public void onSubscribe(@Nonnull Subscription subscription) {
            parent.setSubscription(subscription);
        }

        @Override
        public void onNext(Object value) {
            if (valueReceived) {
                Operators.onNextDropped(value, currentContext());
                return;
            }
            valueReceived = true;
            parent.owner.stageValue(stage, value, parent);
        }

        @Override
        public void onError(Throwable error) {
            parent.fail(error);
        }

        @Override
        public void onComplete() {
            if (!valueReceived) {
                parent.owner.stageEmpty(stage, parent);
            }
        }

        @Override
        public Context currentContext() {
            return parent.currentContext();
        }
    }
}
