package org.jetlinks.core.config;

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
final class MonoConfigRead extends Mono<Value> implements Scannable {

    private final Mono<ConfigStorage> source;
    private final StorageConfigurable owner;
    private final String key;
    private final boolean fallbackParent;

    private MonoConfigRead(Mono<ConfigStorage> source,
                           StorageConfigurable owner,
                           String key,
                           boolean fallbackParent) {
        this.source = Objects.requireNonNull(source, "source");
        this.owner = owner;
        this.key = key;
        this.fallbackParent = fallbackParent;
    }

    static Mono<Value> create(Mono<ConfigStorage> source,
                              StorageConfigurable owner,
                              String key,
                              boolean fallbackParent) {
        return onAssembly(new MonoConfigRead(source, owner, key, fallbackParent));
    }

    @Override
    public void subscribe(@Nonnull CoreSubscriber<? super Value> actual) {
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
            actual.onSubscribe(Operators.scalarSubscription(actual, value));
        } else {
            completeEmpty(actual);
        }
    }

    private Mono<Value> readConfig(ConfigStorage storage) {
        return storage.getConfig(key);
    }

    private Mono<Value> readParentConfig() {
        return owner.getParent().flatMap(parent -> parent.getConfig(key));
    }

    private void subscribeResult(Mono<Value> result, CoreSubscriber<? super Value> actual) {
        if (fallbackParent) {
            result = result.switchIfEmpty(Mono.defer(this::readParentConfig));
        }
        result.subscribe(actual);
    }

    private void completeEmpty(CoreSubscriber<? super Value> actual) {
        if (fallbackParent) {
            Mono.defer(this::readParentConfig).subscribe(actual);
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
