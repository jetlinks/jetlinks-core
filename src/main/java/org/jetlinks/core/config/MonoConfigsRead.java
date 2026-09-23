package org.jetlinks.core.config;

import org.jetlinks.core.Values;
import reactor.core.CoreSubscriber;
import reactor.core.Scannable;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Operators;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.Callable;

/**
 * 在订阅时合并同步批量配置读取，父级仅查询当前存储缺失的配置。
 * 操作符不保存读取结果，重复订阅仍会读取当前配置。
 *
 * @see StorageConfigurable#getConfigs(Collection, boolean)
 */
final class MonoConfigsRead extends Mono<Values> implements Scannable {

    private final Mono<ConfigStorage> source;
    private final StorageConfigurable owner;
    private final Collection<String> keys;
    private final boolean fallbackParent;

    private MonoConfigsRead(Mono<ConfigStorage> source,
                            StorageConfigurable owner,
                            Collection<String> keys,
                            boolean fallbackParent) {
        this.source = Objects.requireNonNull(source, "source");
        this.owner = owner;
        this.keys = keys;
        this.fallbackParent = fallbackParent;
    }

    static Mono<Values> create(Mono<ConfigStorage> source,
                               StorageConfigurable owner,
                               Collection<String> keys,
                               boolean fallbackParent) {
        return onAssembly(new MonoConfigsRead(source, owner, keys, fallbackParent));
    }

    @Override
    public void subscribe(@Nonnull CoreSubscriber<? super Values> actual) {
        if (!(source instanceof Callable)) {
            source.flatMap(this::readConfigs).flatMap(this::resolveValues).subscribe(actual);
            return;
        }

        ConfigStorage storage;
        try {
            storage = (ConfigStorage) ((Callable<?>) source).call();
        } catch (Throwable error) {
            Operators.error(actual, Operators.onOperatorError(error, actual.currentContext()));
            return;
        }

        if (storage == null) {
            Operators.complete(actual);
            return;
        }

        Mono<Values> result;
        Values values;
        try {
            result = Objects.requireNonNull(readConfigs(storage), "The mapper returned a null Publisher");
            if (result instanceof Callable) {
                values = (Values) ((Callable<?>) result).call();
            } else {
                values = null;
            }
        } catch (Throwable error) {
            Operators.error(actual, Operators.onOperatorError(null, error, storage, actual.currentContext()));
            return;
        }

        if (!(result instanceof Callable)) {
            result.flatMap(this::resolveValues).subscribe(actual);
        } else if (values == null) {
            Operators.complete(actual);
        } else {
            subscribeValues(values, actual);
        }
    }

    private Mono<Values> readConfigs(ConfigStorage storage) {
        return storage.getConfigs(keys);
    }

    private Mono<Values> resolveValues(Values values) {
        if (!fallbackParent || keys.isEmpty() || values.size() == keys.size()) {
            return Mono.just(values);
        }
        Collection<String> nonExistent = values.getNonExistentKeys(keys);
        return owner
            .getParent()
            .flatMap(parent -> parent.getConfigs(nonExistent))
            .map(parentValues -> parentValues.merge(values))
            .defaultIfEmpty(values);
    }

    private void subscribeValues(Values values, CoreSubscriber<? super Values> actual) {
        if (!fallbackParent || keys.isEmpty() || values.size() == keys.size()) {
            actual.onSubscribe(Operators.scalarSubscription(actual, values));
            return;
        }
        resolveValues(values).subscribe(actual);
    }

    @Override
    public Object scanUnsafe(@Nonnull Attr attribute) {
        if (attribute == Attr.PARENT) {
            return source;
        }
        return null;
    }
}
