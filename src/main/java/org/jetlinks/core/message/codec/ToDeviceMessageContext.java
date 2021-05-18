package org.jetlinks.core.message.codec;

import org.jetlinks.core.device.DeviceOperator;
import org.jetlinks.core.message.DeviceMessage;
import org.jetlinks.core.message.Message;
import org.jetlinks.core.server.session.DeviceSession;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

/**
 * 发送给设备的上下文,在设备已经在平台中建立会话后,平台下发的指令都会使用此上下文接口
 *
 * @author zhouhao
 * @since 1.0.0
 */
public interface ToDeviceMessageContext extends MessageEncodeContext {

    /**
     * 直接发送消息给设备
     *
     * @param message 消息
     * @return 是否成功
     */
    Mono<Boolean> sendToDevice(@Nonnull EncodedMessage message);

    /**
     * 断开设备与平台的连接
     *
     * @return void
     */
    Mono<Void> disconnect();

    /**
     * @return 获取设备会话
     */
    @Nonnull
    DeviceSession getSession();

    /**
     * 获取指定设备的会话
     *
     * @param deviceId 设备ID
     * @return mono
     */
    Mono<DeviceSession> getSession(String deviceId);

    /**
     * 使用新的消息和设备，转换为新上下文.
     * 通常用于在网关设备协议中,调用子设备协议时.通过此方法将上下为变换为对子设备对操作上下文.
     *
     * @param anotherMessage 设备消息
     * @param device         设备操作接口
     * @return 上下文
     */
    @Override
    default ToDeviceMessageContext mutate(Message anotherMessage, DeviceOperator device) {
        return new ToDeviceMessageContext() {
            @Override
            public Mono<Boolean> sendToDevice(@Nonnull EncodedMessage message) {
                return ToDeviceMessageContext.this.sendToDevice(message);
            }

            @Override
            public Mono<Void> disconnect() {
                return ToDeviceMessageContext.this.disconnect();
            }

            @Nonnull
            @Override
            public DeviceSession getSession() {
                return ToDeviceMessageContext.this.getSession();
            }

            @Override
            public Mono<DeviceSession> getSession(String deviceId) {
                return ToDeviceMessageContext.this.getSession(deviceId);
            }

            @Override
            public Map<String, Object> getConfiguration() {
                return ToDeviceMessageContext.this.getConfiguration();
            }

            @Override
            public Optional<Object> getConfig(String key) {
                return ToDeviceMessageContext.this.getConfig(key);
            }

            @Nonnull
            @Override
            public Message getMessage() {
                return anotherMessage;
            }

            @Override
            public Mono<DeviceOperator> getDevice(String deviceId) {
                return ToDeviceMessageContext.this.getDevice(deviceId);
            }

            @Nullable
            @Override
            public DeviceOperator getDevice() {
                return device;
            }

            @Nonnull
            @Override
            public Mono<Void> reply(@Nonnull DeviceMessage... messages) {
                return ToDeviceMessageContext.this.reply(messages);
            }

            @Nonnull
            @Override
            public Mono<Void> reply(@Nonnull Collection<? extends DeviceMessage> messages) {
                return ToDeviceMessageContext.this.reply(messages);
            }

            @Nonnull
            @Override
            public Mono<Void> reply(@Nonnull Publisher<? extends DeviceMessage> replyMessage) {
                return ToDeviceMessageContext.this.reply(replyMessage);
            }
        };
    }
}
