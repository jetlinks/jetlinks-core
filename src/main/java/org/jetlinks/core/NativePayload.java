package org.jetlinks.core;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.util.AbstractReferenceCounted;
import io.netty.util.Recycler;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.ReferenceCounted;
import io.netty.util.internal.ObjectPool;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.SneakyThrows;
import org.hswebframework.web.bean.FastBeanCopier;
import org.jetlinks.core.codec.Decoder;
import org.jetlinks.core.codec.Encoder;
import org.jetlinks.core.metadata.Jsonable;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.function.Function;
import java.util.function.Supplier;

@Getter
@Setter
public class NativePayload<T> extends AbstractReferenceCounted implements Payload {

    private T nativeObject;

    private Encoder<T> encoder;

    private volatile Payload ref;

    private static Recycler<NativePayload> POOL = new Recycler<NativePayload>() {
        protected NativePayload newObject(io.netty.util.Recycler.Handle<NativePayload> handle) {
            return new NativePayload(handle);
        }
    };

    private final io.netty.util.Recycler.Handle<NativePayload> handle;

    private NativePayload(io.netty.util.Recycler.Handle<NativePayload> handle) {
        this.handle = handle;
    }

    @Override
    public Payload slice() {
        return ref != null ? ref.slice() : this;
    }

    public static <T> NativePayload<T> of(T nativeObject, Encoder<T> encoder) {
        NativePayload<T> payload = POOL.get();
        payload.setRefCnt(1);
        payload.nativeObject = nativeObject;
        payload.encoder = encoder;
        return payload;
    }

    public static <T> NativePayload<T> of(T nativeObject, Supplier<Payload> bodySupplier) {
        return of(nativeObject, v -> bodySupplier.get());
    }

    @Override
    @SuppressWarnings("all")
    @SneakyThrows
    public <T> T decode(Decoder<T> decoder, boolean release) {
        if (decoder.isDecodeFrom(nativeObject)) {
            return (T) nativeObject;
        }
        Class<T> type = decoder.forType();
        if (type == JSONObject.class || type == Map.class) {
            return (T) bodyToJson();
        }
        if (Map.class.isAssignableFrom(decoder.forType())) {
            return bodyToJson().toJavaObject(decoder.forType());
        }
        return Payload.super.decode(decoder, release);
    }

    @Override
    public Object decode() {
        return nativeObject;
    }

    @Override
    public Object decode(boolean release) {
        return nativeObject;
    }

    @Nonnull
    @Override
    public ByteBuf getBody() {
        if (ref == null) {
            synchronized (this) {
                if (ref != null) {
                    return ref.getBody();
                }
                int refCnt = refCnt();
                if (refCnt == 0) {
                    throw new IllegalStateException("refCnt 0");
                }
                ref = encoder.encode(nativeObject);
                if (refCnt > 1) {
                    ref.retain(refCnt - 1);
                }
            }
        }
        return ref.getBody();
    }

    @Override
    public int refCnt() {
        return super.refCnt();
    }


    @Override
    protected void deallocate() {
        try {
            if (ref != null) {
                ref.release();
            }
            this.ref = null;
            this.nativeObject = null;
            this.encoder = null;
        } finally {
            handle.recycle(this);
        }
    }

    @Override
    public NativePayload<T> touch(Object o) {

        return this;
    }

    @Override
    public NativePayload<T> touch() {
        super.touch();
        return this;
    }

    @Override
    public NativePayload<T> retain() {
        return retain(1);
    }

    @Override
    public NativePayload<T> retain(int inc) {
        if (ref != null) {
            ref.retain(inc);
        }
        super.retain(inc);
        return this;
    }

    @Override
    public boolean release() {
        return this.release(1);
    }

    @Override
    public boolean release(int decrement) {
        if (ref != null) {
            ref.release(decrement);
        }
        return super.release(decrement);
    }

    @Override
    public String bodyToString(boolean release) {
        return nativeObject.toString();
    }

    @Override
    @SuppressWarnings("all")
    public JSONArray bodyToJsonArray(boolean release) {
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
    }

    @Override
    public JSONObject bodyToJson(boolean release) {
        if (nativeObject == null) {
            return new JSONObject();
        }
        if (nativeObject instanceof Jsonable) {
            return ((Jsonable) nativeObject).toJson();
        }
        return FastBeanCopier.copy(nativeObject, JSONObject::new);
    }

    @Override
    public String toString() {
        return nativeObject == null ? "null" : nativeObject.toString();
    }
}
