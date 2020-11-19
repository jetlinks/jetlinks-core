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
    ByteBuf getBody();

    default Payload slice() {
        return Payload.of(getBody().slice());
    }

    default <T> T decode(Decoder<T> decoder, boolean release) {
        try {
            return decoder.decode(this);
        } finally {
            if (release) {
                release();
            }
        }
    }

    default <T> T decode(Decoder<T> decoder) {
        return decode(decoder, true);
    }

    default <T> T decode(Class<T> decoder) {
        return decode(decoder, true);
    }

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

    default <T> T convert(Function<ByteBuf, T> mapper) {
        return convert(mapper, true);
    }

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

    default Payload retain() {
        return retain(1);
    }

    default Payload retain(int inc) {
        getBody().retain(inc);
        return this;
    }

    default boolean release(int dec) {
        if (refCnt() >= dec) {
            return ReferenceCountUtil.release(getBody(), dec);
        }
        return true;
    }

    default boolean release() {
        return release(1);
    }

    default byte[] getBytes() {
        return getBytes(true);
    }

    default byte[] getBytes(boolean release) {
        return convert(ByteBufUtil::getBytes, release);
    }

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

    default JSONObject bodyToJson(boolean release) {
        return decode(JSONObject.class);
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

    @Override
    default int refCnt() {
        return getBody().refCnt();
    }

    @Override
    default Payload touch() {
        getBody().touch();
        return this;
    }

    @Override
    default Payload touch(Object o) {
        getBody().touch(o);
        return this;
    }

    Payload voidPayload = () -> Unpooled.EMPTY_BUFFER;

    static Payload of(ByteBuf body) {
        return () -> body;
    }

    static Payload of(byte[] body) {
        return of(Unpooled.wrappedBuffer(body));
    }

    static Payload of(String body) {
        return of(body.getBytes());
    }

    static <T> Payload of(T body, Encoder<T> encoder) {
        return NativePayload.of(body, encoder);
    }
}
