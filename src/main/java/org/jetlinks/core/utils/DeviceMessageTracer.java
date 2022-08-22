package org.jetlinks.core.utils;

import org.jetlinks.core.message.Headers;
import org.jetlinks.core.message.Message;

@Deprecated
public class DeviceMessageTracer {

    public static void trace(Message message, String name) {
        trace(message, name, System.currentTimeMillis());
    }

    public static void trace(Message message, String name, Object value) {
        if (message.getHeaderOrDefault(Headers.enableTrace)) {
            message.addHeader("_trace:" + name, value);
        }
    }

}
