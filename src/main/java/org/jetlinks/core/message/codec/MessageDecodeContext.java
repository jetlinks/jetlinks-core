package org.jetlinks.core.message.codec;


import javax.annotation.Nonnull;

/**
 * @author zhouhao
 * @since 1.0.0
 */
public interface MessageDecodeContext extends MessageCodecContext {

    @Nonnull
    EncodedMessage getMessage();

}
