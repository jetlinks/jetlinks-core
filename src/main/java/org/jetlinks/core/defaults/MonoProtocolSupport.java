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
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

/**
 * 在同步配置热路径中直接解析协议，设备自身未配置或协议源为空时再读取产品协议。
 * 每次订阅重新读取当前配置，不保存配置值、产品或协议结果。
 */
final class MonoProtocolSupport extends Mono<ProtocolSupport> implements Scannable {

    private static final int STAGE_READY = 0;
    private static final int STAGE_RESOLVING = 1;
    private static final int STAGE_STORAGE = 2;
    private static final int STAGE_CONFIG = 3;
    private static final int STAGE_SUPPORT = 4;
    private static final int STAGE_PRODUCT = 5;
    private static final int STAGE_PRODUCT_SUPPORT = 6;
    private static final int STAGE_EMITTING = 7;
    private static final int STAGE_DONE = 8;
    private static final int STAGE_MASK = 0b1111;
    private static final int CANCELLED = 1 << 4;

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
        if (!subscription.beginStage(stage)) {
            return;
        }
        try {
            stageSource.subscribe(new StageSubscriber(subscription, stage));
        } catch (Throwable error) {
            subscription.fail(Operators.onOperatorError(error, subscription.currentContext()), stage);
        }
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

        @SuppressWarnings("rawtypes")
        private static final AtomicIntegerFieldUpdater<ProtocolSubscription> STATE =
            AtomicIntegerFieldUpdater.newUpdater(ProtocolSubscription.class, "state");

        @SuppressWarnings("rawtypes")
        private static final AtomicReferenceFieldUpdater<ProtocolSubscription, Subscription> SUBSCRIPTION =
            AtomicReferenceFieldUpdater.newUpdater(
                ProtocolSubscription.class,
                Subscription.class,
                "subscription"
            );

        private final CoreSubscriber<? super ProtocolSupport> actual;
        private final MonoProtocolSupport owner;

        private volatile Subscription subscription;
        private volatile int state = STAGE_READY;

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
            if (transitionStage(STAGE_READY, STAGE_RESOLVING)) {
                owner.resolve(this);
            }
        }

        @Override
        public void cancel() {
            for (; ; ) {
                int currentState = state;
                if (isCancelled(currentState) || stage(currentState) == STAGE_DONE) {
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
                next.request(Long.MAX_VALUE);
                return;
            }
        }

        private void complete(ProtocolSupport support) {
            if (!transitionStage(STAGE_RESOLVING, STAGE_EMITTING)) {
                Operators.onDiscard(support, currentContext());
                return;
            }
            actual.onNext(support);
            if (transitionStage(STAGE_EMITTING, STAGE_DONE)) {
                SUBSCRIPTION.set(this, null);
                actual.onComplete();
            }
        }

        private void complete() {
            if (transitionStage(STAGE_RESOLVING, STAGE_DONE)) {
                SUBSCRIPTION.set(this, null);
                actual.onComplete();
            }
        }

        private void fail(Throwable error) {
            fail(error, STAGE_RESOLVING);
        }

        private void fail(Throwable error, int expectedStage) {
            if (!transitionStage(expectedStage, STAGE_DONE)) {
                Operators.onErrorDropped(error, currentContext());
                return;
            }
            Subscription current = SUBSCRIPTION.getAndSet(this, null);
            if (current != null) {
                current.cancel();
            }
            actual.onError(error);
        }

        private boolean beginStage(int nextStage) {
            return transitionStage(STAGE_RESOLVING, nextStage);
        }

        private void stageNext(int expectedStage, Object value) {
            if (!transitionStage(expectedStage, STAGE_RESOLVING)) {
                Operators.onNextDropped(value, currentContext());
                return;
            }
            SUBSCRIPTION.set(this, null);
            try {
                owner.stageValue(expectedStage, value, this);
            } catch (Throwable error) {
                fail(Operators.onOperatorError(null, error, value, currentContext()), STAGE_RESOLVING);
            }
        }

        private void stageComplete(int expectedStage) {
            if (!transitionStage(expectedStage, STAGE_RESOLVING)) {
                return;
            }
            SUBSCRIPTION.set(this, null);
            try {
                owner.stageEmpty(expectedStage, this);
            } catch (Throwable error) {
                fail(Operators.onOperatorError(error, currentContext()), STAGE_RESOLVING);
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
                return stage(state) == STAGE_DONE;
            }
            return null;
        }
    }

    private static final class StageSubscriber implements CoreSubscriber<Object> {

        private final ProtocolSubscription parent;
        private final int stage;

        private StageSubscriber(ProtocolSubscription parent, int stage) {
            this.parent = parent;
            this.stage = stage;
        }

        @Override
        public void onSubscribe(@Nonnull Subscription subscription) {
            parent.setSubscription(subscription, stage);
        }

        @Override
        public void onNext(Object value) {
            parent.stageNext(stage, value);
        }

        @Override
        public void onError(Throwable error) {
            parent.fail(error, stage);
        }

        @Override
        public void onComplete() {
            parent.stageComplete(stage);
        }

        @Override
        public Context currentContext() {
            return parent.currentContext();
        }
    }
}
