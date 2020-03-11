package org.jetlinks.core.message.codec;

import io.netty.buffer.ByteBuf;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class SimpleEncodedMessage implements EncodedMessage {

    private ByteBuf payload;

    private MessagePayloadType payloadType;

    public static SimpleEncodedMessage of(ByteBuf byteBuf, MessagePayloadType payloadType) {
        return new SimpleEncodedMessage(byteBuf, payloadType);
    }

}
