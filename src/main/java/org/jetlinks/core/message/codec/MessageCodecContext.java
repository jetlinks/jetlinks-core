package org.jetlinks.core.message.codec;


import org.jetlinks.core.device.DeviceOperator;

import javax.annotation.Nullable;

/**
 * @author zhouhao
 * @since 1.0.0
 */
public interface MessageCodecContext {

    @Nullable
    DeviceOperator getDevice();
}
