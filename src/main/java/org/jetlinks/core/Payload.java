package org.jetlinks.core;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import org.jetlinks.core.codec.Codecs;
import org.jetlinks.core.codec.Decoder;

import javax.annotation.Nonnull;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;

/**
 * 消息负载
 *
 * @author zhouhao
 * @since 1.1
 */
public interface Payload {

    @Nonnull
    ByteBuf getBody();

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
        return decode(decoder, false);
    }

    default <T> T decode(Class<T> decoder) {
        return decode(Codecs.lookup(decoder), false);
    }

    default Object decode() {
        byte[] payload = getBytes();
        //maybe json
        if (/* { }*/(payload[0] == 123 && payload[payload.length - 1] == 125)
                || /* [ ] */(payload[0] == 91 && payload[payload.length - 1] == 93)
        ) {
            return JSON.parse(new String(payload));
        }
        return decode(Object.class);
    }

    default <T> T convert(Function<ByteBuf, T> mapper) {
        return convert(mapper, false);
    }

    default <T> T convert(Function<ByteBuf, T> mapper, boolean release) {
        ByteBuf body = getBody();
        try {
            return mapper.apply(body);
        } finally {
            if (release) {
                release();
            }
        }
    }

    default void retain() {
        getBody().retain();
    }

    default void retain(int inc) {
        getBody().retain(inc);
    }

    default void release(int dec) {
        getBody().release(dec);
    }

    default void release() {
        getBody().release();
    }

    default byte[] getBytes() {
        return getBytes(false);
    }

    default byte[] getBytes(boolean release) {
        return convert(ByteBufUtil::getBytes, release);
    }

    default byte[] getBytes(int offset, int length, boolean release) {
        return convert(byteBuf -> ByteBufUtil.getBytes(byteBuf, offset, length), release);
    }

    default String bodyToString() {
        return getBody().toString(StandardCharsets.UTF_8);
    }

    default JSONObject bodyToJson() {
        return decode(JSONObject.class);
    }

    default JSONArray bodyToJsonArray() {
        return decode(JSONArray.class);
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

}
