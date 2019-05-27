package org.jetlinks.core.message.codec;


import org.jetlinks.core.device.DeviceOperation;

/**
 * @author zhouhao
 * @since 1.0.0
 */
public interface MessageCodecContext {
    DeviceOperation getDeviceOperation();
}
