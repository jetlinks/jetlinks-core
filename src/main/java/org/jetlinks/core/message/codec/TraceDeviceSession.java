package org.jetlinks.core.message.codec;

import lombok.AllArgsConstructor;
import org.jetlinks.core.device.DeviceOperator;
import org.jetlinks.core.server.session.DeviceSession;
import org.jetlinks.core.trace.DeviceTracer;
import org.jetlinks.core.trace.MonoTracer;
import reactor.core.publisher.Mono;

import javax.annotation.Nullable;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Optional;

@AllArgsConstructor(staticName = "of")
public class TraceDeviceSession implements DeviceSession {
    private final DeviceSession target;

    @Override
    public String getId() {
        return target.getId();
    }

    @Override
    public String getDeviceId() {
        return target.getDeviceId();
    }

    @Nullable
    @Override
    public DeviceOperator getOperator() {
        return target.getOperator();
    }

    @Override
    public long lastPingTime() {
        return target.lastPingTime();
    }

    @Override
    public long connectTime() {
        return target.connectTime();
    }

    @Override
    public Mono<Boolean> send(EncodedMessage encodedMessage) {
        return target
                .send(encodedMessage)
                .as(MonoTracer
                            .create(DeviceTracer.SpanName.downstream0(getDeviceId()),
                                    (builder -> builder.setAttribute(DeviceTracer.SpanKey.message, encodedMessage.toString())))
                );
    }

    @Override
    public Transport getTransport() {
        return target.getTransport();
    }

    @Override
    public void close() {
        target.close();
    }

    @Override
    public void ping() {
        target.ping();
    }

    @Override
    public boolean isAlive() {
        return target.isAlive();
    }

    @Override
    public void onClose(Runnable call) {
        target.onClose(call);
    }

    @Override
    public Optional<InetSocketAddress> getClientAddress() {
        return target.getClientAddress();
    }

    @Override
    public <T extends DeviceSession> T unwrap(Class<T> type) {
        return target.unwrap(type);
    }

    @Override
    public boolean isWrapFrom(Class<?> type) {
        return target.isWrapFrom(type);
    }

    @Override
    public Optional<String> getServerId() {
        return target.getServerId();
    }

    @Override
    public Duration getKeepAliveTimeout() {
        return target.getKeepAliveTimeout();
    }

    @Override
    public Mono<Boolean> isAliveAsync() {
        return target.isAliveAsync();
    }

    @Override
    public Mono<Boolean> send(ToDeviceMessageContext context) {
        return target.send(context);
    }
}
