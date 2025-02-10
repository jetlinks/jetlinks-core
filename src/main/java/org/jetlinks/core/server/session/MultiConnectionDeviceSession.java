package org.jetlinks.core.server.session;

import io.netty.util.internal.ThreadLocalRandom;
import lombok.AllArgsConstructor;
import lombok.Generated;
import lombok.Getter;
import org.jetlinks.core.device.DeviceOperator;
import org.jetlinks.core.device.session.DeviceSessionManager;
import org.jetlinks.core.enums.ErrorCode;
import org.jetlinks.core.exception.DeviceOperationException;
import org.jetlinks.core.message.codec.EncodedMessage;
import org.jetlinks.core.message.codec.Transport;
import org.jetlinks.core.server.ClientConnection;
import reactor.core.Disposable;
import reactor.core.Disposables;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 支持多个客户端连接的设备会话
 *
 * @param <C> 客户端连接类型
 * @author zhouhao
 * @since 1.2.3
 */
@AllArgsConstructor
public abstract class MultiConnectionDeviceSession<C extends ClientConnection>
    extends CopyOnWriteArrayList<C>
    implements DeviceSession {
    private final Disposable.Composite disposable = Disposables.composite();

    @Getter
    @Generated
    private final String id;

    @Getter
    @Generated
    private transient final DeviceOperator operator;

    protected transient final DeviceSessionManager sessionManager;

    @Override
    public String getDeviceId() {
        return id;
    }

    @Override
    public abstract long lastPingTime();

    @Override
    public abstract long connectTime();

    public void registerConnection(C connection) {
        if (!connection.isAlive()) {
            return;
        }

        if (addIfAbsent(connection)) {
            connection.onDisconnect(() -> this.handleDisconnect(connection));
        }

    }

    private void handleDisconnect(C connection) {
        unregisterConnection(connection);
        //check session
        sessionManager
            .getSession(getDeviceId(), true)
            .subscribe();
    }

    protected void unregisterConnection(C connection) {
        synchronized (this) {
            this.remove(connection);
        }
    }

    @Override
    public Mono<Boolean> send(EncodedMessage encodedMessage) {
        C connection = takeConnection();
        if (connection == null) {
            return Mono.error(new DeviceOperationException.NoStackTrace(ErrorCode.CONNECTION_LOST));
        }
        return Mono
            .defer(() -> connection.sendMessage(encodedMessage))
            .thenReturn(true);
    }

    @Override
    public abstract Transport getTransport();

    @Override
    public void close() {
        disposable.dispose();
        synchronized (this) {
            for (C conn : this) {
                conn.disconnect();
            }
            clear();
        }
    }

    @Override
    public abstract void ping();

    @Override
    public boolean isAlive() {
        if (disposable.isDisposed()) {
            return false;
        }
        boolean alive = false;
        for (C conn : this) {
            alive |= conn.isAlive();
        }
        return alive;
    }

    @Override
    public void onClose(Runnable call) {
        disposable.add(call::run);
    }

    @Override
    public Optional<InetSocketAddress> getClientAddress() {
        return Optional
            .ofNullable(takeConnection())
            .map(ClientConnection::address);
    }

    @Override
    public abstract void setKeepAliveTimeout(Duration timeout);


    protected final C takeConnection() {
        C connection;
        do {
            synchronized (this) {
                int size = this.size();
                if (size == 0) {
                    return null;
                }
                if (size == 1) {
                    connection = this.get(0);
                } else {
                    connection = this.get(ThreadLocalRandom.current().nextInt(size));
                }
            }
            if (connection.isAlive()) {
                return connection;
            }
            connection.disconnect();
            handleDisconnect(connection);
        } while (true);

    }
}
