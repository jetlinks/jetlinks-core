package org.jetlinks.core.server.session;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jetlinks.core.device.DeviceOperator;
import org.jetlinks.core.message.codec.EncodedMessage;
import org.jetlinks.core.message.codec.ToDeviceMessageContext;
import org.jetlinks.core.message.codec.Transport;
import org.jetlinks.core.utils.Reactors;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;


@Slf4j
public class ChildrenDeviceSession implements DeviceSession, ReplaceableDeviceSession {
    @Getter
    private final String id;

    @Getter
    private final String deviceId;

    @Getter
    private DeviceSession parent;

    @Getter
    private final DeviceOperator operator;

    private List<Runnable> closeListener;

    private long lastKeepAliveTime;

    private long keepAliveTimeOutMs = -1;

    private BiConsumer<DeviceSession, DeviceSession> parentChanged;

    public ChildrenDeviceSession(String deviceId, DeviceSession parent, DeviceOperator operator) {
        this.id = deviceId;
        this.parent = parent;
        this.operator = operator;
        this.deviceId = deviceId;
        this.lastKeepAliveTime = parent.lastPingTime();

    }

    public DeviceOperator getParentDevice() {
        return parent.getOperator();
    }

    @Override
    public long lastPingTime() {
        return lastKeepAliveTime;
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
        this.lastKeepAliveTime = System.currentTimeMillis();
    }

    @Override
    public void keepAlive() {
        parent.keepAlive();
        this.lastKeepAliveTime = System.currentTimeMillis();
    }

    private boolean aliveByKeepAlive() {
        return keepAliveTimeOutMs <= 0
            || System.currentTimeMillis() - lastKeepAliveTime < keepAliveTimeOutMs;
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
    public Optional<InetSocketAddress> getClientAddress() {
        return parent.getClientAddress();
    }

    @Override
    public void setKeepAliveTimeout(Duration timeout) {
        keepAliveTimeOutMs = timeout.toMillis();
    }

    @Override
    public Duration getKeepAliveTimeout() {
        return Duration.ofMillis(keepAliveTimeOutMs);
    }

    @Override
    public boolean isWrapFrom(Class<?> type) {
        return type.isInstance(this) || parent.isWrapFrom(type);
    }

    @Override
    public <T extends DeviceSession> T unwrap(Class<T> type) {
        return type.isInstance(this) ? type.cast(this) : parent.unwrap(type);
    }

    @Override
    public boolean isAlive() {
        if (keepAliveTimeOutMs > 0 && aliveByKeepAlive()) {
            return true;
        }
        return parent.isAlive();
    }

    @Override
    public Mono<Boolean> isAliveAsync() {
        //使用心跳时间来保活
        if (keepAliveTimeOutMs > 0 && aliveByKeepAlive()) {
            return Reactors.ALWAYS_TRUE;
        }
        //判断上级
        return parent.isAliveAsync();
    }

    @Override
    public boolean isChanged(DeviceSession another) {
        return parent.isChanged(another);
    }

    @Override
    public String toString() {
        return "children device[" + deviceId + "] in " + parent;
    }

    public synchronized void doOnParentChanged(BiConsumer<DeviceSession, DeviceSession> consumer) {
        this.parentChanged = consumer;
    }

    @Override
    public void replaceWith(DeviceSession session) {
        if (session == this || Objects.equals(session.getDeviceId(), this.getDeviceId())) {
            throw new IllegalStateException("can not replace with self");
        }
        DeviceSession old = this.parent;
        if (session.isWrapFrom(ChildrenDeviceSession.class)) {
            this.parent = session.unwrap(ChildrenDeviceSession.class).getParent();
        } else {
            this.parent = session;
        }
        if (parentChanged != null) {
            parentChanged.accept(old, this.parent);
        }
    }

    @Override
    public Mono<Boolean> send(ToDeviceMessageContext context) {
        return parent.send(context);
    }
}
