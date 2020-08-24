package org.jetlinks.core.server.session;

import org.jetlinks.core.device.DeviceOperator;
import org.jetlinks.core.message.codec.EncodedMessage;
import org.jetlinks.core.message.codec.Transport;
import reactor.core.publisher.Mono;

import javax.annotation.Nullable;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Optional;

/**
 * @author zhouhao
 * @see ChildrenDeviceSession
 * @since 1.0.0
 */
public interface DeviceSession {

    String getId();

    String getDeviceId();

    @Nullable
    DeviceOperator getOperator();

    long lastPingTime();

    long connectTime();

    Mono<Boolean> send(EncodedMessage encodedMessage);

    Transport getTransport();

    void close();

    void ping();

    boolean isAlive();

    void onClose(Runnable call);

    default Optional<String> getServerId() {
        return Optional.empty();
    }

    default Optional<InetSocketAddress> getClientAddress() {
        return Optional.empty();
    }

    default void keepAlive() {
        ping();
    }

    default void setKeepAliveTimeout(Duration timeout) {

    }

    default boolean isWrapFrom(Class<?> type) {
        return type.isInstance(this);
    }

    default <T extends DeviceSession> T unwrap(Class<T> type) {
        return type.cast(this);
    }
}
