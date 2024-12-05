package org.jetlinks.core.server.session;

import io.netty.util.ReferenceCountUtil;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.jetlinks.core.device.DeviceOperator;
import org.jetlinks.core.enums.ErrorCode;
import org.jetlinks.core.exception.DeviceOperationException;
import org.jetlinks.core.message.Headers;
import org.jetlinks.core.message.codec.EncodedMessage;
import org.jetlinks.core.message.codec.ToDeviceMessageContext;
import org.jetlinks.core.message.codec.Transport;
import org.jetlinks.core.utils.Reactors;
import reactor.core.publisher.Mono;

import javax.annotation.Nullable;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Optional;

public class KeepOnlineSession implements DeviceSession, ReplaceableDeviceSession, PersistentSession {

    @Getter
    DeviceSession parent;

    @Setter(AccessLevel.PACKAGE)
    private long lastKeepAliveTime = System.currentTimeMillis();

    private final long connectTime = System.currentTimeMillis();

    //忽略上级会话信息,设置为true后. 设备是否离线以超时时间为准
    @Setter
    @Getter
    private boolean ignoreParent = Headers.keepOnlineIgnoreParent.getDefaultValue();

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
        return Mono
            .defer(() -> {
                if (parent.isAlive()) {
                    return parent.send(encodedMessage);
                }
                ReferenceCountUtil.safeRelease(encodedMessage.getPayload());
                return Mono.error(new DeviceOperationException.NoStackTrace(ErrorCode.CONNECTION_LOST));
            });
    }

    @Override
    public Transport getTransport() {
        return parent.getTransport();
    }

    @Override
    public void close() {
        if (!ignoreParent) {
            parent.close();
        }
    }

    @Override
    public void ping() {
        lastKeepAliveTime = System.currentTimeMillis();
        parent.keepAlive();
    }

    @Override
    public boolean isAlive() {
        if (aliveByKeepAlive()) {
            return true;
        }
        if (ignoreParent) {
            return false;
        }
        return parent.isAlive();
    }

    private boolean aliveByKeepAlive() {
        return keepAliveTimeOutMs <= 0
            || System.currentTimeMillis() - lastKeepAliveTime < keepAliveTimeOutMs;
    }

    @Override
    public void onClose(Runnable call) {
        parent.onClose(call);
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
        parent.setKeepAliveTimeout(timeout);
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
    public void replaceWith(DeviceSession session) {
        this.parent = session;
    }

    @Override
    public String getProvider() {
        return KeepOnlineDeviceSessionProvider.ID;
    }

    @Override
    public Mono<Boolean> isAliveAsync() {
        //心跳正常
        if (aliveByKeepAlive()) {
            return Reactors.ALWAYS_TRUE;
        }
        //忽略上级
        if (ignoreParent) {
            return Reactors.ALWAYS_FALSE;
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
        return "keepOnline[" + keepAliveTimeOutMs + "ms]:" + parent;
    }

    @Override
    public Mono<Boolean> send(ToDeviceMessageContext context) {
        return parent.send(context);
    }
}
