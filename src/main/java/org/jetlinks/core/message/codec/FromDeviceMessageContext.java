package org.jetlinks.core.message.codec;


import org.jetlinks.core.device.DeviceOperator;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.message.DeviceMessage;
import org.jetlinks.core.server.ClientConnection;
import org.jetlinks.core.server.session.DeviceSession;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Function;

/**
 * 来自设备的消息上下文，可以通过此上下文获取设备会话
 *
 * @author zhouhao
 * @since 1.0.0
 */
public interface FromDeviceMessageContext extends MessageDecodeContext {
    /**
     * 获取设备会话
     *
     * @return 设备会话
     */
    DeviceSession getSession();

    /**
     * 获取连接信息,在1.2.3版本后,一个设备可能存在多个连接,此方法用于获取当前连接信息.
     * <p>
     * 如果返回null,说明当前接入方式不支持多连接,或者无法获取连接信息.
     *
     * @return 连接信息
     * @since 1.2.3
     */
    default ClientConnection getConnection() {
        return null;
    }

    /**
     * @see MessageDecodeContext#getDevice()
     */
    @Override
    @Nullable
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
                                       Function<DeviceMessage, Mono<Void>> handler) {
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

    static FromDeviceMessageContext of(DeviceSession session,
                                       EncodedMessage message,
                                       DeviceRegistry registry,
                                       ClientConnection connection,
                                       Function<DeviceMessage, Mono<Void>> handler) {
        return new FromDeviceMessageContext() {
            @Override
            public ClientConnection getConnection() {
                return connection;
            }

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
