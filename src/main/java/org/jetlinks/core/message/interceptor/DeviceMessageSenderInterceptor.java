package org.jetlinks.core.message.interceptor;

import org.jetlinks.core.device.CompositeDeviceMessageSenderInterceptor;
import org.jetlinks.core.device.DeviceOperator;
import org.jetlinks.core.message.DeviceMessage;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 设备消息发送拦截器,用于在消息发送端拦截处理消息.
 * 可用于在一些自定义回复逻辑的场景
 *
 * @see org.jetlinks.core.device.DeviceMessageSender
 * @since 1.0.0
 */
public interface DeviceMessageSenderInterceptor {

    DeviceMessageSenderInterceptor DO_NOTING = new DeviceMessageSenderInterceptor() {
    };

    /**
     * 在消息发送前触发. 执行此方法后将使用返回值{@link DeviceMessage}进行发送到设备.
     *
     * @param device  设备操作接口
     * @param message 消息对象
     * @return 新的消息
     */
    default Mono<DeviceMessage> preSend(DeviceOperator device, DeviceMessage message) {
        return Mono.just(message);
    }

    /**
     * 执行发送时触发.
     *
     * @param device 设备操作接口
     * @param source 指令
     * @param sender 消息发送逻辑
     * @return 新的发送逻辑
     */
    default Flux<DeviceMessage> doSend(DeviceOperator device, DeviceMessage source, Flux<DeviceMessage> sender) {
        return sender;
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
    default <R extends DeviceMessage> Flux<R> afterSent(DeviceOperator device, DeviceMessage message, Flux<R> reply) {
        return reply;
    }

    /**
     * 组合多个拦截器
     * @param interceptor interceptor
     * @return 新的拦截器
     */
    default DeviceMessageSenderInterceptor andThen(DeviceMessageSenderInterceptor interceptor) {
        if (this == DO_NOTING) {
            return interceptor;
        }
        CompositeDeviceMessageSenderInterceptor composite = new CompositeDeviceMessageSenderInterceptor();
        composite.addInterceptor(this);
        composite.addInterceptor(interceptor);
        return composite;
    }

    /**
     * 排序序号,值小的在前,大的再后.
     *
     * @return 序号
     */
    default int getOrder() {
        return Integer.MAX_VALUE;
    }
}
