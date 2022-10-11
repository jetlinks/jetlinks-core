package org.jetlinks.core;

import org.jetlinks.core.message.HeaderKey;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Supplier;

public class GenericHeaderSupport<SELF extends GenericHeaderSupport<SELF>> implements HeaderSupport<SELF> {

    private volatile Map<String, Object> headers;

    private Map<String, Object> safeGetHeader() {
        if (headers == null) {
            synchronized (this) {
                if (headers == null) {
                    headers = new ConcurrentHashMap<>(64);
                }
            }
        }
        return headers;
    }

    public final void setHeaders(Map<String, Object> headers) {
        if (headers != null && !(headers instanceof ConcurrentHashMap)) {
            headers = new ConcurrentHashMap<>(headers);
        }
        this.headers = headers;
    }

    @Nullable
    @Override
    public final Map<String, Object> getHeaders() {
        return headers;
    }

    @Override
    public final SELF addHeader(String header, Object value) {
        if (header == null || value == null) {
            return castSelf();
        }
        safeGetHeader().put(header, value);
        return castSelf();
    }

    @Override
    public final SELF addHeaderIfAbsent(String header, Object value) {
        if (header == null || value == null) {
            return castSelf();
        }
        safeGetHeader().putIfAbsent(header, value);
        return castSelf();
    }

    @Override
    public <T> SELF addHeaderIfAbsent(HeaderKey<T> header, T value) {
        return HeaderSupport.super.addHeaderIfAbsent(header, value);
    }

    @Override
    public final SELF removeHeader(String header) {
        if (null != header) {
            safeGetHeader().remove(header);
        }
        return castSelf();
    }

    @Override
    public final Object computeHeader(String key, BiFunction<String, Object, Object> computer) {
        return safeGetHeader()
                .compute(key, computer);
    }

    @Override
    public Object getHeaderOrElse(String header, @Nullable Supplier<Object> orElse) {
        return HeaderSupport.super.getHeaderOrElse(header, orElse);
    }

    @Override
    public Optional<Object> getHeader(String header) {
        return HeaderSupport.super.getHeader(header);
    }

    @Override
    public <T> Optional<T> getHeader(HeaderKey<T> key) {
        return HeaderSupport.super.getHeader(key);
    }

    @Override
    public <T> T getHeaderOrDefault(HeaderKey<T> key) {
        return HeaderSupport.super.getHeaderOrDefault(key);
    }

    @Override
    public <T> T getHeaderOrElse(HeaderKey<T> header, @Nullable Supplier<T> orElse) {
        return HeaderSupport.super.getHeaderOrElse(header, orElse);
    }

    @Override
    public <T> T getOrAddHeader(HeaderKey<T> header, Supplier<T> value) {
        return HeaderSupport.super.getOrAddHeader(header, value);
    }

    @Override
    public <T> T getOrAddHeaderDefault(HeaderKey<T> header) {
        return HeaderSupport.super.getOrAddHeaderDefault(header);
    }

    @Override
    public <T> T computeHeader(HeaderKey<T> key, BiFunction<String, T, T> computer) {
        return HeaderSupport.super.computeHeader(key, computer);
    }

    @Override
    public <T> SELF addHeader(HeaderKey<T> header, T value) {
        return HeaderSupport.super.addHeader(header, value);
    }
}
