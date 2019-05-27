package org.jetlinks.core.message.codec;


/**
 * @author zhouhao
 * @since 1.0.0
 */
public interface MessageDecodeContext extends MessageCodecContext {

    EncodedMessage getMessage();

}
