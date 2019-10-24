package org.jetlinks.core.device;

import org.jetlinks.core.message.DeviceMessageReply;

public interface ReplyFailureHandler {

    void handle(Throwable err, DeviceMessageReply message);
}
