package org.jetlinks.core;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import org.jetlinks.core.codec.Codecs;

import javax.annotation.Nonnull;
import java.nio.charset.StandardCharsets;

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

    default byte[] bodyAsBytes() {
        return ByteBufUtil.getBytes(getBody());
    }

    default byte[] bodyAsBytes(int offset, int length) {
        return ByteBufUtil.getBytes(getBody(), offset, length);
    }

    default String bodyAsString() {
        return getBody().toString(StandardCharsets.UTF_8);
    }

    Payload voidPayload = () -> Unpooled.EMPTY_BUFFER;

    static Payload of(ByteBuf body) {
        return () -> body;
    }
}
