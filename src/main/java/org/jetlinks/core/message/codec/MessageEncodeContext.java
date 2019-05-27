package org.jetlinks.core.message.codec;

import org.jetlinks.core.message.DeviceMessage;

/**
 * @author zhouhao
 * @since 1.0.0
 */
public interface MessageEncodeContext extends MessageCodecContext {

    DeviceMessage getMessage();

}
