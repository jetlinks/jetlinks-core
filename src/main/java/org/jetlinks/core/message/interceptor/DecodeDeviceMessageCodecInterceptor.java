package org.jetlinks.core.message.interceptor;

import org.jetlinks.core.message.DeviceMessage;
import org.jetlinks.core.message.codec.MessageDecodeContext;

public interface DecodeDeviceMessageCodecInterceptor extends DeviceMessageCodecInterceptor {

    void preDecode(MessageDecodeContext message);

    DeviceMessage postDecode(MessageDecodeContext message, DeviceMessage deviceMessage);

}
