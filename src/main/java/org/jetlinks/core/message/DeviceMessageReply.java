package org.jetlinks.core.message;

import org.jetlinks.core.enums.ErrorCode;


/**
 * @author zhouhao
 * @since 1.0.0
 */
public interface DeviceMessageReply extends DeviceMessage {
    boolean isSuccess();

    String getCode();

    String getMessage();

    DeviceMessageReply error(ErrorCode errorCode);

    DeviceMessageReply from(DeviceMessage message);

    DeviceMessageReply messageId(String messageId);
}
