package org.jetlinks.core.codec.defaults;

import com.alibaba.fastjson.JSON;
import org.jetlinks.core.Payload;
import org.jetlinks.core.codec.Codec;
import org.jetlinks.core.metadata.Jsonable;

import javax.annotation.Nonnull;

public class JsonCodec<T> implements Codec<T> {

    private final Class<? extends T> type;

    private JsonCodec(Class<? extends T> type) {
        this.type = type;
    }

    public static <T> JsonCodec<T> of(Class<? extends T> type) {
        return new JsonCodec<>(type);
    }

    @Override
    public Class<T> forType() {
        return (Class<T>) type;
    }

    @Override
    public T decode(@Nonnull Payload payload) {
        return JSON.parseObject(payload.getBytes(false), type);
    }

    @Override
    public Payload encode(T body) {
        if(body instanceof Jsonable){
            return Payload.of(JSON.toJSONBytes(((Jsonable) body).toJson()));
        }
        return Payload.of(JSON.toJSONBytes(body));
    }

}
