package org.jetlinks.core.message;

import lombok.Getter;
import lombok.Setter;

import javax.annotation.Nonnull;

/**
 * 透传设备消息
 *
 * @author zhouhao
 * @see 1.0.2
 */
@Getter
@Setter
public class DirectDeviceMessage extends CommonDeviceMessage {

    @Nonnull
    private byte[] payload;

    @Override
    public MessageType getMessageType() {
        return MessageType.DIRECT;
    }
}
