package org.jetlinks.core.trace;

import com.google.common.collect.Maps;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import lombok.RequiredArgsConstructor;
import org.hswebframework.web.exception.TraceSourceException;
import org.jetlinks.core.utils.ExceptionUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.function.BiConsumer;

@RequiredArgsConstructor
class ErrorAttributes implements Attributes {

    final static AttributeKey<String> ATTR_EXCEPTION_OPERATION = AttributeKey.stringKey("exception.operation");
    final static AttributeKey<String> ATTR_EXCEPTION_SOURCE = AttributeKey.stringKey("exception.source");
    final static AttributeKey<String> ATTR_EXCEPTION_TYPE = AttributeKey.stringKey("exception.type");
    final static AttributeKey<String> ATTR_EXCEPTION_MESSAGE = AttributeKey.stringKey("exception.message");
    final static AttributeKey<String> ATTR_EXCEPTION_STACK = AttributeKey.stringKey("exception.stacktrace");

    final Throwable error;

    volatile Map<AttributeKey<?>, Object> parsed;

    @Nullable
    @Override
    @SuppressWarnings("all")
    public <T> T get(@Nonnull AttributeKey<T> key) {
        return (T) asMap().get(key);
    }

    @Override
    public void forEach(@Nonnull BiConsumer<? super AttributeKey<?>, ? super Object> consumer) {
        asMap().forEach(consumer);
    }

    @Override
    public int size() {
        return asMap().size();
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public Map<AttributeKey<?>, Object> asMap() {
        if (parsed == null) {
            synchronized (this) {
                if (parsed == null) {
                    return parsed = parse();
                }
            }
        }
        return parsed;
    }

    private Map<AttributeKey<?>, Object> parse() {
        Map<AttributeKey<?>, Object> container = Maps.newHashMapWithExpectedSize(5);
        container.put(ATTR_EXCEPTION_MESSAGE, error.getLocalizedMessage());
        container.put(ATTR_EXCEPTION_TYPE, error.getClass().getCanonicalName());
        container.put(ATTR_EXCEPTION_STACK, ExceptionUtils.getStackTrace(error));

        //记录错误源信息
        String operation = TraceSourceException.tryGetOperation(error);
        Object source = TraceSourceException.tryGetSource(error);
        if (operation != null) {
            container.put(ATTR_EXCEPTION_OPERATION, operation);
        }
        if (source != null) {
            container.put(ATTR_EXCEPTION_SOURCE, source);
        }
        return container;
    }

    @Override
    public AttributesBuilder toBuilder() {
        return Attributes.builder().putAll(this);
    }
}
