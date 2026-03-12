package org.jetlinks.core.message.codec;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetlinks.core.utils.CharsetUtils;

import java.nio.charset.StandardCharsets;

@AllArgsConstructor
@Getter
public class SimpleEncodedMessage implements EncodedMessage {

    private final ByteBuf payload;

    private final MessagePayloadType payloadType;

    public SimpleEncodedMessage(ByteBuf payload, MessagePayloadType payloadType) {
        this.payload = payload;
        this.payloadType = payloadType;
    }

    public static SimpleEncodedMessage of(ByteBuf byteBuf, MessagePayloadType payloadType) {
        return new SimpleEncodedMessage(byteBuf, payloadType);
    }

    @JsonIgnore
    @Getter(AccessLevel.PRIVATE)
    private transient String _toString;

    @Override
    public String toString() {
        if (_toString != null) {
            return _toString;
        }
        if (payload == null || !payload.isReadable()) {
            return "<released>";
        }
        return _toString = ByteBufUtil.hexDump(payload);
    }
}
