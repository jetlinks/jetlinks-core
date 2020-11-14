package org.jetlinks.core;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.util.ReferenceCountUtil;
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
@NoArgsConstructor
public class NativePayload<T> implements Payload {

    private static final AtomicIntegerFieldUpdater<NativePayload> retainUpdater =
            AtomicIntegerFieldUpdater.newUpdater(NativePayload.class, "retainCount");

    private static final AtomicIntegerFieldUpdater<NativePayload> releaseUpdater =
            AtomicIntegerFieldUpdater.newUpdater(NativePayload.class, "releaseCount");


    private T nativeObject;

    private Encoder<T> encoder;

    private volatile Payload ref;

    private volatile int retainCount = 0;
    private volatile int releaseCount = 0;

    @Override
    public Payload slice() {
        return ref != null ? ref.slice() : this;
    }

    public static <T> NativePayload<T> of(T nativeObject, Encoder<T> encoder) {
        NativePayload<T> payload = new NativePayload<>();

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
                Payload buf = encoder.encode(nativeObject);
                if (retainCount > 0) {
                    buf.retain(retainCount);
                    retainUpdater.set(this, 0);
                }
                if (releaseCount > 0) {
                    buf.release(releaseCount);
                    releaseUpdater.set(this, 0);
                }
                ref = buf;
            }
        }
        return ref.getBody();
    }

    @Override
    public void release(int dec) {
        //synchronized (this) {
            if (ref != null) {
                ref.release(releaseUpdater.getAndSet(this, 0) + dec);
            } else {
                releaseUpdater.addAndGet(this, dec);
            }
       // }
    }

    @Override
    public void retain(int inc) {
      //  synchronized (this) {
            if (ref != null) {
                ref.retain(releaseUpdater.getAndSet(this, 0) + inc);
            } else {
                retainUpdater.addAndGet(this, inc);
            }
       // }
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
