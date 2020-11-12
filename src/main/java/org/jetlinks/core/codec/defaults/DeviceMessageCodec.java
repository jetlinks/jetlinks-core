package org.jetlinks.core.codec.defaults;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import io.netty.buffer.Unpooled;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetlinks.core.Payload;
import org.jetlinks.core.codec.Codec;
import org.jetlinks.core.message.Message;
import org.jetlinks.core.message.MessageType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class DeviceMessageCodec implements Codec<Message> {
    public static DeviceMessageCodec INSTANCE = new DeviceMessageCodec();

    @Override
    public Class<Message> forType() {
        return Message.class;
    }

    @Nullable
    @Override
    public Message decode(@Nonnull Payload payload) {
        JSONObject json = JSON.parseObject(payload.bodyToString());
        return MessageType
                .convertMessage(json)
                .orElseThrow(() -> new UnsupportedOperationException("unsupported message : " + json));
    }

    @Override
    public Payload encode(Message body) {
        return Payload.of(Unpooled.wrappedBuffer(JSON.toJSONBytes(body.toJson())));
    }
}
