package org.jetlinks.core.server.session;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jetlinks.core.device.DeviceOperator;
import org.jetlinks.core.message.codec.EncodedMessage;
import org.jetlinks.core.message.codec.Transport;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

@Getter
@Slf4j
public class ChildrenDeviceSession implements DeviceSession {
    private final String id;

    private final String deviceId;

    private final DeviceSession parent;

    private final DeviceOperator operator;

    private List<Runnable> closeListener;

    public ChildrenDeviceSession(String deviceId, DeviceSession parent, DeviceOperator operator) {
        this.id = deviceId;
        this.parent = parent;
        this.operator = operator;
        this.deviceId = deviceId;
    }

    @Override
    public long lastPingTime() {
        return parent.lastPingTime();
    }

    @Override
    public long connectTime() {
        return parent.connectTime();
    }

    @Override
    public Mono<Boolean> send(EncodedMessage encodedMessage) {
        log.info("send child device[{}:{}] message", parent.getDeviceId(), deviceId);
        return parent.send(encodedMessage);
    }

    @Override
    public Transport getTransport() {
        return parent.getTransport();
    }

    @Override
    public void close() {
        if (null != closeListener) {
            closeListener.forEach(Runnable::run);
        }
    }

    @Override
    public void ping() {
        parent.ping();
    }

    @Override
    public boolean isAlive() {
        return parent.isAlive();
    }

    @Override
    public synchronized void onClose(Runnable call) {
        if (closeListener == null) {
            closeListener = new CopyOnWriteArrayList<>();
        }
        closeListener.add(call);
    }

    @Override
    public Optional<String> getServerId() {
        return parent.getServerId();
    }

    @Override
    public boolean isWrapFrom(Class<?> type) {
        return type == ChildrenDeviceSession.class || parent.isWrapFrom(type);
    }

    @Override
    public <T extends DeviceSession> T unwrap(Class<T> type) {
        return type == ChildrenDeviceSession.class ? type.cast(this) : parent.unwrap(type);
    }

    @Override
    public String toString() {
        return "children device[" + deviceId + "] in " + getParent();
    }
}
