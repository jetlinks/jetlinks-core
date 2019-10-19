package org.jetlinks.core.message.codec;

import org.jetlinks.core.message.DeviceMessage;

import java.nio.ByteBuffer;

public interface DeviceMessageDeserializer {
    DeviceMessage deserialize(ByteBuffer byteBuffer);
}
