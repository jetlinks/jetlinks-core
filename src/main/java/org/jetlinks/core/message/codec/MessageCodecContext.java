package org.jetlinks.core.message.codec;


import org.jetlinks.core.device.DeviceOperator;
import org.jetlinks.core.message.DeviceMessage;
import org.jetlinks.core.message.Message;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import javax.annotation.Nullable;

/**
 * @author zhouhao
 * @since 1.0.0
 */
public interface MessageCodecContext {

    @Nullable
    DeviceOperator getDevice();
}
