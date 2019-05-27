package org.jetlinks.core.device;

import org.jetlinks.core.message.*;

import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @author zhouhao
 * @since 1.0.0
 */
public interface DeviceMessageSender {

    <R extends DeviceMessageReply> CompletionStage<R> retrieveReply(String deviceId, String messageId, Supplier<R> replyNewInstance);

    <R extends DeviceMessageReply> CompletionStage<R> send(RepayableDeviceMessage<R> message);

    <R extends DeviceMessageReply> CompletionStage<R> send(DeviceMessage message, Function<Object, R> replyMapping);

    FunctionInvokeMessageSender invokeFunction(String function);

    ReadPropertyMessageSender readProperty(String... property);

    WritePropertyMessageSender writeProperty();

}
