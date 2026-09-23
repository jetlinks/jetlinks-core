package org.jetlinks.core.config;

import org.jetlinks.core.Configurable;
import org.jetlinks.core.Value;
import reactor.core.CoreSubscriber;
import reactor.core.Scannable;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Operators;

import javax.annotation.Nonnull;
import java.util.Objects;
import java.util.concurrent.Callable;

/**
 * 在订阅时合并同步配置读取，异步来源及父级回退仍由 Reactor 管理。
 * 操作符只保存查询参数，不保存配置值或跨订阅的可变状态。
 *
 * @see StorageConfigurable#getConfig(String, boolean)
 */
final class MonoConfigRead<T> extends Mono<T> implements Scannable {

    private final Mono<ConfigStorage> source;
    private final StorageConfigurable owner;
    private final Object key;
    private final boolean fallbackParent;

    private MonoConfigRead(Mono<ConfigStorage> source,
                           StorageConfigurable owner,
                           Object key,
                           boolean fallbackParent) {
        this.source = Objects.requireNonNull(source, "source");
        this.owner = owner;
        this.key = Objects.requireNonNull(key, "key");
        this.fallbackParent = fallbackParent;
    }

    static Mono<Value> create(Mono<ConfigStorage> source,
                              StorageConfigurable owner,
                              String key,
                              boolean fallbackParent) {
        return onAssembly(new MonoConfigRead<Value>(source, owner, key, fallbackParent));
    }

    static <V> Mono<V> create(Mono<ConfigStorage> source,
                              StorageConfigurable owner,
                              ConfigKey<V> key,
                              boolean fallbackParent) {
        return onAssembly(new MonoConfigRead<>(source, owner, key, fallbackParent));
    }

    @Override
    public void subscribe(@Nonnull CoreSubscriber<? super T> actual) {
        if (!(source instanceof Callable)) {
            subscribeResult(source.flatMap(this::readConfig), actual);
            return;
        }

        ConfigStorage storage;
        try {
            storage = ConfigStorage.class.cast(((Callable<?>) source).call());
        } catch (Throwable error) {
            Operators.error(actual, Operators.onOperatorError(error, actual.currentContext()));
            return;
        }

        if (storage == null) {
            completeEmpty(actual);
            return;
        }

        Mono<Value> result;
        Value value;
        try {
            result = Objects.requireNonNull(readConfig(storage), "The mapper returned a null Publisher");
            if (result instanceof Callable) {
                value = Value.class.cast(((Callable<?>) result).call());
            } else {
                value = null;
            }
        } catch (Throwable error) {
            Operators.error(actual, Operators.onOperatorError(null, error, storage, actual.currentContext()));
            return;
        }

        if (!(result instanceof Callable)) {
            subscribeResult(result, actual);
        } else if (value != null) {
            emitValue(actual, value);
        } else {
            completeEmpty(actual);
        }
    }

    private Mono<Value> readConfig(ConfigStorage storage) {
        return storage.getConfig(keyName());
    }

    private Mono<T> readParentConfig() {
        return owner.getParent().flatMap(this::readParentConfig);
    }

    @SuppressWarnings("unchecked")
    private Mono<? extends T> readParentConfig(Configurable parent) {
        return key instanceof ConfigKey
            ? parent.getConfig((ConfigKey<T>) key)
            : (Mono<? extends T>) parent.getConfig((String) key);
    }

    @SuppressWarnings("unchecked")
    private void subscribeResult(Mono<Value> result, CoreSubscriber<? super T> actual) {
        Mono<T> source;
        if (key instanceof ConfigKey) {
            source = result.mapNotNull(this::convertValue);
        } else {
            source = (Mono<T>) result;
        }
        if (fallbackParent) {
            source = source.switchIfEmpty(Mono.defer(this::readParentConfig));
        }
        source.subscribe(actual);
    }

    private void emitValue(CoreSubscriber<? super T> actual, Value value) {
        T result;
        try {
            result = convertValue(value);
        } catch (Throwable error) {
            Operators.error(actual, Operators.onOperatorError(null, error, value, actual.currentContext()));
            return;
        }
        if (result == null) {
            Operators.complete(actual);
        } else {
            actual.onSubscribe(Operators.scalarSubscription(actual, result));
        }
    }

    @SuppressWarnings("unchecked")
    private T convertValue(Value value) {
        return !(key instanceof ConfigKey)
            ? (T) value
            : value.as(((ConfigKey<T>) key).getValueType());
    }

    private String keyName() {
        return key instanceof ConfigKey
            ? ((ConfigKey<?>) key).getKey()
            : (String) key;
    }

    private void completeEmpty(CoreSubscriber<? super T> actual) {
        if (fallbackParent) {
            Mono<T> parent;
            try {
                parent = Objects.requireNonNull(readParentConfig(), "The mapper returned a null Publisher");
            } catch (Throwable error) {
                Operators.error(actual, Operators.onOperatorError(error, actual.currentContext()));
                return;
            }
            parent.subscribe(actual);
        } else {
            Operators.complete(actual);
        }
    }

    @Override
    public Object scanUnsafe(@Nonnull Attr attribute) {
        if (attribute == Attr.PARENT) {
            return source;
        }
        return null;
    }
}
