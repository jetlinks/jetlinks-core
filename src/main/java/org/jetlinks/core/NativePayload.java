package org.jetlinks.core;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import io.netty.buffer.ByteBuf;
import io.netty.util.ReferenceCountUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.hswebframework.web.bean.FastBeanCopier;
import org.jetlinks.core.codec.Decoder;
import org.jetlinks.core.codec.Encoder;
import org.jetlinks.core.metadata.Jsonable;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.function.Supplier;

@Getter
@Setter
@Slf4j
@Deprecated
public class NativePayload<T> implements Payload {

    private T nativeObject;

    private Encoder<T> encoder;

    private volatile Payload ref;

    private ByteBuf buf;

    private NativePayload() {
    }

    @Override
    public Payload slice() {
        return ref != null ? ref.slice() : this;
    }

    public static <T> NativePayload<T> of(T nativeObject, Encoder<T> encoder) {
        NativePayload<T> payload = new NativePayload<>();
        ;
        payload.nativeObject = nativeObject;
        payload.encoder = encoder;
        return payload;
    }

    public static <T> NativePayload<T> of(T nativeObject) {
        return of(nativeObject, (Encoder<T>) null);
    }

    public static <T> NativePayload<T> of(T nativeObject, Supplier<Payload> bodySupplier) {
        return of(nativeObject, v -> bodySupplier.get());
    }

    @Override
    @SuppressWarnings("all")
    @SneakyThrows
    public <T> T decode(Decoder<T> decoder, boolean release) {
        try {
            if (decoder.isDecodeFrom(nativeObject)) {
                return (T) nativeObject;
            }
        } finally {
            if (release) {
                ReferenceCountUtil.safeRelease(this);
            }
        }
        Class<T> type = decoder.forType();
        if (type == JSONObject.class || type == Map.class) {
            return (T) bodyToJson(release);
        }
        if (Map.class.isAssignableFrom(decoder.forType())) {
            return bodyToJson(release).toJavaObject(decoder.forType());
        }
        return Payload.super.decode(decoder, release);
    }

    @SuppressWarnings("all")
    @Override
    public <T> T decode(Class<T> type) {
        if (type.isInstance(nativeObject)) {
            return (T) nativeObject;
        }
        if (type == JSONObject.class || type == Map.class) {
            return (T) bodyToJson();
        }
        if (Map.class.isAssignableFrom(type)) {
            return bodyToJson().toJavaObject(type);
        }
        return FastBeanCopier.copy(nativeObject, type);
    }

    @Override
    public Object decode() {
        return decode(true);
    }

    @Override
    public Object decode(boolean release) {
        try {
            return nativeObject;
        } finally {
            if (release) {
                ReferenceCountUtil.safeRelease(this);
            }
        }
    }

    @Nonnull
    @Override
    public ByteBuf getBody() {
        return null;
    }

    @Override
    public int refCnt() {
        return 0;
    }

    @Override
    public NativePayload<T> touch(Object o) {
        return this;
    }

    @Override
    public NativePayload<T> touch() {

        return this;
    }

    @Override
    public NativePayload<T> retain() {
        return retain(1);
    }

    @Override
    public NativePayload<T> retain(int inc) {

        return this;
    }

    @Override
    public boolean release() {
        return this.release(1);
    }

    @Override
    public boolean release(int decrement) {
        return true;
    }

    @Override
    public String bodyToString(boolean release) {
        try {
            return String.valueOf(nativeObject);
        } finally {
            if (release) {
                ReferenceCountUtil.safeRelease(this);
            }
        }
    }

    @Override
    @SuppressWarnings("all")
    public JSONArray bodyToJsonArray(boolean release) {
        try {
            if (nativeObject == null) {
                return new JSONArray();
            }
            if (nativeObject instanceof JSONArray) {
                return ((JSONArray) nativeObject);
            }
            List<Object> collection;
            if (nativeObject instanceof List) {
                collection = ((List<Object>) nativeObject);
            } else if (nativeObject instanceof Collection) {
                collection = new ArrayList<>(((Collection<Object>) nativeObject));
            } else if (nativeObject instanceof Object[]) {
                collection = Arrays.asList(((Object[]) nativeObject));
            } else {
                throw new UnsupportedOperationException("body is not arry");
            }
            return new JSONArray(collection);
        } finally {
            if (release) {
                ReferenceCountUtil.safeRelease(this);
            }
        }
    }

    @Override
    @SuppressWarnings("all")
    public JSONObject bodyToJson(boolean release) {
        try {
            if (nativeObject == null) {
                return new JSONObject();
            }
            if (nativeObject instanceof Jsonable) {
                return ((Jsonable) nativeObject).toJson();
            }
            if (nativeObject instanceof JSONObject) {
                return ((JSONObject) nativeObject);
            }
            if (nativeObject instanceof Map) {
                return new JSONObject(((Map) nativeObject));
            }
            return FastBeanCopier.copy(nativeObject, JSONObject::new);
        } finally {
            if (release) {
                ReferenceCountUtil.safeRelease(this);
            }
        }
    }

    @Override
    public String toString() {
        return nativeObject == null ? "null" : nativeObject.toString();
    }

//    @Override
//    protected void finalize() throws Throwable {
//        if (refCnt() > 0) {
//            log.warn("payload {} was not release properly, release() was not called before it's garbage-collected. refCnt={}", nativeObject, refCnt());
//        }
//        super.finalize();
//    }
}
