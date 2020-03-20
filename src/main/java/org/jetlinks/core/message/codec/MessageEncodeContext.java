package org.jetlinks.core.message.codec;

import org.jetlinks.core.message.Message;

import javax.annotation.Nonnull;

/**
 * @author zhouhao
 * @since 1.0.0
 * @see ToDeviceMessageContext
 */
public interface MessageEncodeContext extends MessageCodecContext {

    @Nonnull
    Message getMessage();

}
