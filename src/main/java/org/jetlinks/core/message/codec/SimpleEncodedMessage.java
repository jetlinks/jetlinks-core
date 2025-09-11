package org.jetlinks.core.message.codec;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetlinks.core.utils.CharsetUtils;

import java.nio.charset.StandardCharsets;

@AllArgsConstructor
@Getter
public class SimpleEncodedMessage implements EncodedMessage {

    private final ByteBuf payload;

    private final MessagePayloadType payloadType;

    public static SimpleEncodedMessage of(ByteBuf byteBuf, MessagePayloadType payloadType) {
        return new SimpleEncodedMessage(byteBuf, payloadType);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        ByteBufUtil.appendPrettyHexDump(builder, payload);
        return builder.toString();
    }
}
