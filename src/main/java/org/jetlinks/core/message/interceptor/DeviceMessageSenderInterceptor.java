package org.jetlinks.core.message.interceptor;

import org.jetlinks.core.device.DeviceOperation;
import org.jetlinks.core.message.DeviceMessage;
import org.jetlinks.core.message.DeviceMessageReply;

import java.util.concurrent.CompletionStage;

public interface DeviceMessageSenderInterceptor {

    DeviceMessage preSend(DeviceOperation device, DeviceMessage message);

   <R extends DeviceMessageReply> CompletionStage<R> afterReply(DeviceOperation device, DeviceMessage message, R reply);

}
