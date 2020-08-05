package org.jetlinks.core.codec.defaults;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import io.netty.buffer.Unpooled;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetlinks.core.Payload;
import org.jetlinks.core.codec.Codec;
import org.jetlinks.core.message.DeviceMessage;
import org.jetlinks.core.message.MessageType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class DeviceMessageCodec implements Codec<DeviceMessage> {
    public static DeviceMessageCodec INSTANCE = new DeviceMessageCodec();

    @Override
    public Class<DeviceMessage> forType() {
        return DeviceMessage.class;
    }

    @Nullable
    @Override
    public DeviceMessage decode(@Nonnull Payload payload) {
        JSONObject json = JSON.parseObject(payload.bodyToString());
        return MessageType
                .convertMessage(json)
                .map(DeviceMessage.class::cast)
                .orElseThrow(() -> new UnsupportedOperationException("unsupport device message : " + json));
    }

    @Override
    public Payload encode(DeviceMessage body) {
        return Payload.of(Unpooled.wrappedBuffer(body.toJson().toJSONString().getBytes()));
    }
}
