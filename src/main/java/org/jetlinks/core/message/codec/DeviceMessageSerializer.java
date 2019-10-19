package org.jetlinks.core.message.codec;

import org.jetlinks.core.message.DeviceMessage;

import java.nio.ByteBuffer;

public interface DeviceMessageSerializer {
    ByteBuffer serialize(DeviceMessage deviceMessage);
}
