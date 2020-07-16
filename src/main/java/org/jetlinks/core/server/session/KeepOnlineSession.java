package org.jetlinks.core.server.session;

import org.jetlinks.core.device.DeviceOperator;
import org.jetlinks.core.message.codec.EncodedMessage;
import org.jetlinks.core.message.codec.Transport;
import reactor.core.publisher.Mono;

import javax.annotation.Nullable;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Optional;

public class KeepOnlineSession implements DeviceSession {

    DeviceSession parent;

    private long lastKeepAliveTime = System.currentTimeMillis();

    private long connectTime = System.currentTimeMillis();

    private long keepAliveTimeOutMs;

    public KeepOnlineSession(DeviceSession parent, Duration keepAliveTimeOut) {
        this.parent = parent;
        setKeepAliveTimeout(keepAliveTimeOut);
    }

    @Override
    public String getId() {
        return parent.getId();
    }

    @Override
    public String getDeviceId() {
        return parent.getDeviceId();
    }

    @Nullable
    @Override
    public DeviceOperator getOperator() {
        return parent.getOperator();
    }

    @Override
    public long lastPingTime() {
        return lastKeepAliveTime;
    }

    @Override
    public long connectTime() {
        return connectTime;
    }

    @Override
    public Mono<Boolean> send(EncodedMessage encodedMessage) {
        return Mono.defer(() -> {
            if (parent.isAlive()) {
                return parent.send(encodedMessage);
            }
            return Mono.just(false);
        });
    }

    @Override
    public Transport getTransport() {
        return parent.getTransport();
    }

    @Override
    public void close() {

    }

    @Override
    public void ping() {
        lastKeepAliveTime = System.currentTimeMillis();
        parent.keepAlive();
    }

    @Override
    public boolean isAlive() {
        return keepAliveTimeOutMs <= 0 || System.currentTimeMillis() - lastKeepAliveTime < keepAliveTimeOutMs;
    }

    @Override
    public void onClose(Runnable call) {
        parent.onClose(call);
    }

    @Override
    public Optional<InetSocketAddress> getClientAddress() {
        return parent.getClientAddress();
    }

    @Override
    public void setKeepAliveTimeout(Duration timeout) {
        keepAliveTimeOutMs = timeout.toMillis();
        parent.setKeepAliveTimeout(timeout);
    }
}
