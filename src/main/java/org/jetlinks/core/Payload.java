package org.jetlinks.core;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.util.ReferenceCountUtil;
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

    default <T> T bodyAs(Class<T> type) {
        return Codecs.lookup(type).decode(this);
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

    default <T> T convert(Function<ByteBuf, T> mapper) {
        return convert(mapper, true);
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

    default byte[] bodyAsBytes() {
        return bodyAsBytes(true);
    }

    default byte[] bodyAsBytes(boolean release) {
        return convert(ByteBufUtil::getBytes, release);
    }

    default byte[] bodyAsBytes(int offset, int length, boolean release) {
        return convert(byteBuf -> ByteBufUtil.getBytes(byteBuf, offset, length), release);
    }

    default String bodyAsString() {
        return getBody().toString(StandardCharsets.UTF_8);
    }

    Payload voidPayload = () -> Unpooled.EMPTY_BUFFER;

    static Payload of(ByteBuf body) {
        return () -> body;
    }
}
