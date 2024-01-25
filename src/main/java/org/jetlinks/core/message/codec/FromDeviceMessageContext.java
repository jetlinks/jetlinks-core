package org.jetlinks.core.message.codec;


import org.jetlinks.core.device.DeviceOperator;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.message.DeviceMessage;
import org.jetlinks.core.server.session.DeviceSession;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import java.util.function.Function;

/**
 * 来自设备的消息上下文，可以通过此上下文获取设备会话
 *
 * @author zhouhao
 * @since 1.0.0
 */
public interface FromDeviceMessageContext extends MessageDecodeContext {
    DeviceSession getSession();

    @Override
    default DeviceOperator getDevice() {
        return getSession().getOperator();
    }

    static FromDeviceMessageContext of(DeviceSession session,
                                       EncodedMessage message) {
        return new FromDeviceMessageContext() {
            @Override
            public DeviceSession getSession() {
                return session;
            }

            @Nonnull
            @Override
            public EncodedMessage getMessage() {
                return message;
            }
        };
    }

    static FromDeviceMessageContext of(DeviceSession session,
                                       EncodedMessage message,
                                       DeviceRegistry registry) {
        return new FromDeviceMessageContext() {
            @Override
            public DeviceSession getSession() {
                return session;
            }

            @Nonnull
            @Override
            public EncodedMessage getMessage() {
                return message;
            }

            @Override
            public Mono<DeviceOperator> getDevice(String deviceId) {
                return registry.getDevice(deviceId);
            }
        };
    }

    static FromDeviceMessageContext of(DeviceSession session,
                                       EncodedMessage message,
                                       DeviceRegistry registry,
                                       Function<DeviceMessage,Mono<Void>> handler) {
        return new FromDeviceMessageContext() {
            @Override
            public DeviceSession getSession() {
                return session;
            }

            @Nonnull
            @Override
            public EncodedMessage getMessage() {
                return message;
            }

            @Override
            public Mono<DeviceOperator> getDevice(String deviceId) {
                return registry.getDevice(deviceId);
            }

            @Override
            public Mono<Void> handleMessage(DeviceMessage message) {
                return handler.apply(message);
            }
        };
    }
}
