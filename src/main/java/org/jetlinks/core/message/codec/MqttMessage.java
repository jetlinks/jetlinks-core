package org.jetlinks.core.message.codec;

import javax.annotation.Nonnull;

/**
 * @author zhouhao
 * @since 1.0.0
 */
public interface MqttMessage extends EncodedMessage {

    @Nonnull
    String getTopic();

    default int getQosLevel(){
        return 0;
    }

}
