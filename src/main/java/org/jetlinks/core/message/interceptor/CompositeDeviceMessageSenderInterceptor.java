package org.jetlinks.core.message.interceptor;

import org.jetlinks.core.device.DeviceOperation;
import org.jetlinks.core.message.DeviceMessage;
import org.jetlinks.core.message.DeviceMessageReply;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CopyOnWriteArrayList;

public class CompositeDeviceMessageSenderInterceptor implements DeviceMessageSenderInterceptor {
    private List<DeviceMessageSenderInterceptor> interceptors = new CopyOnWriteArrayList<>();

    public void addInterceptor(DeviceMessageSenderInterceptor interceptor) {
        interceptors.add(interceptor);
    }

    @Override
    public DeviceMessage preSend(DeviceOperation device, DeviceMessage message) {
        for (DeviceMessageSenderInterceptor interceptor : interceptors) {
            message = interceptor.preSend(device, message);
        }
        return message;
    }

    @Override
    public <R extends DeviceMessageReply> CompletionStage<R> afterReply(DeviceOperation device, DeviceMessage message, R reply) {

        CompletableFuture<R> future = CompletableFuture.completedFuture(reply);
        for (DeviceMessageSenderInterceptor interceptor : interceptors) {
            future = future.thenCompose(r -> interceptor.afterReply(device, message, r));
        }
        return future;

    }
}
