package org.jetlinks.core.device;

import org.jetlinks.core.message.DeviceMessage;
import org.jetlinks.core.message.interceptor.DeviceMessageSenderInterceptor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class CompositeDeviceMessageSenderInterceptor implements DeviceMessageSenderInterceptor {
    private final List<DeviceMessageSenderInterceptor> interceptors = new CopyOnWriteArrayList<>();

    public void addInterceptor(DeviceMessageSenderInterceptor interceptor) {
        interceptors.add(interceptor);
        //重新排序
        interceptors.sort(Comparator.comparingInt(DeviceMessageSenderInterceptor::getOrder));
    }

    /**
     * 在消息发送前触发. 执行此方法后将使用返回值{@link DeviceMessage}进行发送到设备.
     *
     * @param device  设备操作接口
     * @param message 消息对象
     * @return 新的消息
     */
    @Override
    public Mono<DeviceMessage> preSend(DeviceOperator device, DeviceMessage message) {
        Mono<DeviceMessage> mono = Mono.just(message);

        for (DeviceMessageSenderInterceptor interceptor : interceptors) {
            mono = mono.flatMap(msg -> interceptor.preSend(device, msg));
        }
        return mono;
    }

    /**
     * 执行发送时触发.
     *
     * @param device 设备操作接口
     * @param source 指令
     * @param sender 消息发送逻辑
     * @return 新的发送逻辑
     */
    @Override
    public Flux<DeviceMessage> doSend(DeviceOperator device, DeviceMessage source, Flux<DeviceMessage> sender) {
        Flux<DeviceMessage> flux = sender;

        for (DeviceMessageSenderInterceptor interceptor : interceptors) {
            flux = interceptor.doSend(device, source, flux);
        }
        return flux;
    }

    /**
     * 在消息发送后触发.这里发送后并不是真正的发送，其实只是构造了整个发送的逻辑流{@link Flux}(参数 reply),
     *
     * @param device  设备操作接口
     * @param message 源消息
     * @param reply   回复的消息
     * @param <R>     回复的消息类型
     * @return 新的回复结果
     */
    @Override
    public <R extends DeviceMessage> Flux<R> afterSent(DeviceOperator device, DeviceMessage message, Flux<R> reply) {

        Flux<R> flux = reply;

        for (DeviceMessageSenderInterceptor interceptor : interceptors) {
            flux = interceptor.afterSent(device, message, flux);
        }
        return flux;

    }
}
