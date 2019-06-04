package org.jetlinks.core.message.interceptor;

import org.jetlinks.core.message.DeviceMessage;
import org.jetlinks.core.message.codec.MessageDecodeContext;

public interface DecodeDeviceMessageInterceptor extends DeviceMessageInterceptor {

    void preDecode(MessageDecodeContext message);

    DeviceMessage postDecode(MessageDecodeContext message, DeviceMessage deviceMessage);

}
