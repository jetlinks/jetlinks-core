package org.jetlinks.core;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.ReferenceCounted;
import org.jetlinks.core.codec.Codecs;
import org.jetlinks.core.codec.Decoder;
import org.jetlinks.core.codec.Encoder;

import javax.annotation.Nonnull;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;

/**
 * 消息负载
 *
 * @author zhouhao
 * @since 1.1
 */
public interface Payload extends ReferenceCounted {

    @Nonnull
    @Deprecated
    ByteBuf getBody();

    @Deprecated
    default Payload slice() {
        return Payload.of(getBody().slice());
    }

    @Deprecated
    default <T> T decode(Decoder<T> decoder, boolean release) {
        try {
            return decoder.decode(this);
        } finally {
            if (release) {
                ReferenceCountUtil.safeRelease(this);
            }
        }
    }

    @Deprecated
    default <T> T decode(Decoder<T> decoder) {
        return decode(decoder, true);
    }

    default <T> T decode(Class<T> decoder) {
        return decode(decoder, true);
    }

    @Deprecated
    default <T> T decode(Class<T> decoder, boolean release) {
        return decode(Codecs.lookup(decoder), release);
    }

    default Object decode(boolean release) {
        byte[] payload = getBytes(false);
        //maybe json
        if (/* { }*/(payload[0] == 123 && payload[payload.length - 1] == 125)
                || /* [ ] */(payload[0] == 91 && payload[payload.length - 1] == 93)
        ) {
            try {
                return JSON.parse(new String(payload));
            } finally {
                if (release) {
                    ReferenceCountUtil.safeRelease(this);
                }
            }
        }
        return decode(Object.class, release);
    }

    default Object decode() {
        return decode(true);
    }

    @Deprecated
    default <T> T convert(Function<ByteBuf, T> mapper) {
        return convert(mapper, true);
    }

    @Deprecated
    default <T> T convert(Function<ByteBuf, T> mapper, boolean release) {
        ByteBuf body = getBody();
        try {
            return mapper.apply(body);
        } finally {
            if (release) {
                ReferenceCountUtil.safeRelease(this);
            }
        }
    }

    @Deprecated
    default Payload retain() {
        return retain(1);
    }

    @Deprecated
    default Payload retain(int inc) {
        getBody().retain(inc);
        return this;
    }

    @Deprecated
    default boolean release(int dec) {
        if (refCnt() >= dec) {
            return ReferenceCountUtil.release(getBody(), dec);
        }
        return true;
    }

    @Deprecated
    default boolean release() {
        return release(1);
    }

    @Deprecated
    default byte[] getBytes() {
        return getBytes(true);
    }

    @Deprecated
    default byte[] getBytes(boolean release) {
        return convert(ByteBufUtil::getBytes, release);
    }

    @Deprecated
    default byte[] getBytes(int offset, int length, boolean release) {
        return convert(byteBuf -> ByteBufUtil.getBytes(byteBuf, offset, length), release);
    }

    default String bodyToString() {
        return bodyToString(true);
    }

    default String bodyToString(boolean release) {
        try {
            return getBody().toString(StandardCharsets.UTF_8);
        } finally {
            if (release) {
                ReferenceCountUtil.safeRelease(this);
            }
        }
    }

    @Deprecated
    default JSONObject bodyToJson(boolean release) {
        return decode(JSONObject.class,release);
    }

    default JSONObject bodyToJson() {
        return bodyToJson(true);
    }

    default JSONArray bodyToJsonArray() {
        return bodyToJsonArray(true);
    }

    default JSONArray bodyToJsonArray(boolean release) {
        return decode(JSONArray.class);
    }

    @Deprecated
    @Override
    default int refCnt() {
        return getBody().refCnt();
    }

    @Deprecated
    @Override
    default Payload touch() {
        getBody().touch();
        return this;
    }

    @Deprecated
    @Override
    default Payload touch(Object o) {
        getBody().touch(o);
        return this;
    }

    Payload voidPayload = Payload.of(Unpooled.EMPTY_BUFFER);

    static Payload of(ByteBuf body) {
        return ByteBufPayload.of(body);
    }

    static Payload of(byte[] body) {
        return of(Unpooled.wrappedBuffer(body));
    }

    static Payload of(String body) {
        return of(body.getBytes());
    }

    static <T> Payload of(T body, Encoder<T> encoder) {
        if (body instanceof Payload) {
            return encoder.encode(body);
        }
        return NativePayload.of(body, encoder);
    }
}
