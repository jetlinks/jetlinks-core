package org.jetlinks.core.device.registry;

import org.jetlinks.core.message.DeviceMessage;
import org.jetlinks.core.message.DeviceMessageReply;

import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

/**
 * @author zhouhao
 * @since 1.0.0
 */
public interface DeviceMessageHandler {
    void handleMessage(String serverId, Consumer<DeviceMessage> deviceMessageJson);

    void handleDeviceCheck(String serviceId, Consumer<String> deviceId);

    CompletionStage<Boolean> reply(DeviceMessageReply message);

    void close();
}
