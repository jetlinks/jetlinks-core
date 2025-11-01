package org.jetlinks.core.server.session;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.internal.ThreadLocalRandom;
import jakarta.annotation.Nonnull;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetlinks.core.device.DeviceOperator;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.device.session.DeviceSessionManager;
import org.jetlinks.core.enums.ErrorCode;
import org.jetlinks.core.exception.DeviceOperationException;
import org.jetlinks.core.message.ChildDeviceMessage;
import org.jetlinks.core.message.DeviceMessage;
import org.jetlinks.core.message.Headers;
import org.jetlinks.core.message.codec.DefaultTransport;
import org.jetlinks.core.message.codec.EncodedMessage;
import org.jetlinks.core.message.codec.ToDeviceMessageContext;
import org.jetlinks.core.message.codec.Transport;
import org.jetlinks.core.trace.DeviceTracer;
import org.jetlinks.core.trace.FluxTracer;
import org.jetlinks.core.utils.Reactors;
import reactor.core.Disposable;
import reactor.core.Disposables;
import reactor.core.Scannable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuples;

import javax.annotation.Nullable;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BinaryOperator;

import static org.jetlinks.core.server.session.DeviceSessionSelector.*;

@Slf4j
public class MultiGatewayDeviceSession extends CopyOnWriteArrayList<DeviceSession>
    implements DeviceSession, Disposable, Scannable, PersistentSession {


    private final long connectTime = System.currentTimeMillis();
    private transient final DeviceOperator device;
    private transient final DeviceSessionManager sessionManager;

    private final Composite disposable = Disposables.composite();

    public MultiGatewayDeviceSession(DeviceOperator device, DeviceSessionManager sessionManager) {
        this.device = device;
        this.sessionManager = sessionManager;
    }

    @Override
    public String getId() {
        return device.getDeviceId();
    }

    @Override
    public String getDeviceId() {
        return device.getDeviceId();
    }

    public void register(DeviceSession session) {
        synchronized (this) {
            log.info("register device[{}] session {}", getDeviceId(), session);
            add(session);
        }
    }

    @Nullable
    @Override
    public final DeviceOperator getOperator() {
        return device;
    }

    @Override
    public void setKeepAliveTimeout(Duration timeout) {

        for (DeviceSession session : this) {
            session.setKeepAliveTimeout(timeout);
        }
    }

    @Override
    public Duration getKeepAliveTimeout() {
        for (DeviceSession session : this) {
            return session.getKeepAliveTimeout();
        }
        return Duration.ZERO;
    }

    @Override
    public Optional<InetSocketAddress> getClientAddress() {
        return stream()
            .<InetSocketAddress>mapMulti(
                (s, t) -> s.getClientAddress().ifPresent(t))
            .findAny();
    }

    @Override
    public long lastPingTime() {
        return stream()
            .mapToLong(DeviceSession::lastPingTime)
            .max()
            .orElse(System.currentTimeMillis());
    }

    @Override
    public long connectTime() {
        return connectTime;
    }

    @Override
    public Mono<Boolean> send(ToDeviceMessageContext context) {
        DeviceMessage msg = (DeviceMessage) context.getMessage();

        byte selector = msg.getHeaderOrDefault(Headers.sessionSelector);

        // 发送给所有session
        if (selector == all) {
            return Flux
                .fromIterable(this)
                .concatMap(session -> send(session, msg, context))
                .as(FluxTracer.create(DeviceTracer.SpanName.operation0(getDeviceId(), "downstream_all")))
                .reduce(Boolean::logicalOr);
        }
        // 发送给最近的
        if (selector == latest) {
            return send(
                takeSession(SESSION_LATEST),
                msg, context
            );
        }
        // 发送给最近的
        if (selector == oldest) {
            return send(
                takeSession(SESSION_OLDEST),
                msg, context
            );
        }
        // hash
        if (selector == hashed) {
            return this
                .send(
                    stream()
                        .reduce(hashedOperator(s -> s, msg))
                        .orElseThrow(() -> new DeviceOperationException.NoStackTrace(ErrorCode.CONNECTION_LOST)),
                    msg, context
                );
        }
        // hash
        if (selector == minimumLoad) {
            return this
                .send(takeSession(SESSION_MINIMUM_LOAD), msg, context);
        }
        // 默认随机发送.
        DeviceSession session = takeSession();
        if (session == null) {
            return Mono.error(new DeviceOperationException.NoStackTrace(ErrorCode.CONNECTION_LOST));
        }

        return send(session, msg, context);
    }

    private Mono<Boolean> send(DeviceSession session, DeviceMessage msg, ToDeviceMessageContext context) {
        // 发送给子设备,但是消息不是子设备消息.
        if (session.isWrapFrom(ChildrenDeviceSession.class) && !(msg instanceof ChildDeviceMessage)) {
            ChildrenDeviceSession cds = session.unwrap(ChildrenDeviceSession.class);
            return session
                .send(context.mutate(
                    cds.getParent(),
                    ChildDeviceMessage.create(
                        cds.getParent().getDeviceId(),
                        msg
                    )));
        }
        return session.send(context.mutate(session, msg));
    }

    @Override
    public Mono<Boolean> send(EncodedMessage encodedMessage) {
        // 正常不应该会直接调用到这里.
        return Mono.error(new UnsupportedOperationException("unsupported direct send"));
    }

    private void removeSession(DeviceSession session) {
        synchronized (this) {
            remove(session);
        }
    }

    private void handleClosed(DeviceSession connection) {
        removeSession(connection);
        //check session
        sessionManager
            .getSession(getDeviceId(), true)
            .subscribe();
    }

    protected final DeviceSession takeSession(BinaryOperator<DeviceSession> comparator) {
        return stream()
            .reduce(comparator)
            .orElseThrow(() -> new DeviceOperationException.NoStackTrace(ErrorCode.CONNECTION_LOST));
    }

    protected final DeviceSession takeSession() {
        DeviceSession session;
        do {
            synchronized (this) {
                int size = this.size();
                if (size == 0) {
                    return null;
                }
                if (size == 1) {
                    session = this.get(0);
                } else {
                    session = this.get(ThreadLocalRandom.current().nextInt(size));
                }
            }
            if (session.isAlive()) {
                return session;
            }
            session.close();
            handleClosed(session);
        } while (true);

    }

    @Override
    public Transport getTransport() {
        if (size() == 0) {
            return DefaultTransport.TCP;
        }
        return get(0).getTransport();
    }

    @Override
    public void close() {
        disposable.dispose();
        forEach(DeviceSession::close);
        clear();

    }

    @Override
    public void ping() {
        forEach(DeviceSession::ping);
    }

    @Override
    public boolean isAlive() {
        if (disposable.isDisposed() || isEmpty()) {
            return false;
        }
        boolean anyAlive = false;
        for (DeviceSession session : this) {
            if (session.isAlive()) {
                anyAlive = true;
            } else {
                remove(session);
            }
        }
        return anyAlive;
    }

    @Override
    public boolean isWrapFrom(Class<?> type) {
        if (type.isInstance(this)) {
            return true;
        }
        for (DeviceSession session : this) {
            if (session.isWrapFrom(type)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public <T> T unwrap(Class<T> type) {
        if (type.isInstance(this)) {
            return type.cast(this);
        }
        for (DeviceSession session : this) {
            if (session.isWrapFrom(type)) {
                return session.unwrap(type);
            }
        }
        return type.cast(this);
    }

    @Override
    public Mono<Boolean> isAliveAsync() {
        if (disposable.isDisposed() || isEmpty()) {
            return Reactors.ALWAYS_FALSE;
        }
        return Flux
            .fromIterable(this)
            .filterWhen(session -> session
                .isAliveAsync()
                .doOnNext(alive -> {
                    if (!alive) {
                        // 已失效,移除会话
                        remove(session);
                    }
                }))
            .hasElements();
    }

    @Override
    public void onClose(Runnable call) {
        disposable.add(call::run);
    }

    @Override
    public boolean isDisposed() {
        return disposable.isDisposed();
    }

    @Override
    public void dispose() {
        disposable.dispose();
    }

    @Override
    public String toString() {
        return "MultiGateway" + super.toString();
    }

    @Override
    public Object scanUnsafe(@Nonnull Attr key) {
        // 只支持获取buffer
        if (key == Attr.BUFFERED) {
            return this
                .stream()
                .mapToInt(conn -> Scannable.from(conn).scan(Attr.BUFFERED))
                .sum();
        }

        return null;
    }

    @Override
    public String getProvider() {
        return Provider.ID;
    }

    @AllArgsConstructor
   public static class Provider implements DeviceSessionProvider {
        static final String ID = "multi_gateway";
        private final DeviceSessionManager sessionManager;

        @Override
        public String getId() {
            return ID;
        }

        @Override
        public Mono<PersistentSession> deserialize(byte[] sessionData, DeviceRegistry registry) {
            try {
                ByteBuf buf = Unpooled.wrappedBuffer(sessionData);
                byte[] deviceId = new byte[buf.readUnsignedShort()];
                buf.readBytes(deviceId);
                return registry
                    .getDevice(new String(deviceId))
                    .<PersistentSession>flatMap(device -> {
                        MultiGatewayDeviceSession session = new MultiGatewayDeviceSession(device, sessionManager);
                        List<Mono<?>> sessions = new ArrayList<>(2);
                        while (buf.isReadable()) {
                            byte[] provider = new byte[buf.readUnsignedShort()];
                            buf.readBytes(provider);
                            byte[] data = new byte[buf.readInt()];
                            buf.readBytes(data);
                            DeviceSessionProviders
                                .lookup(new String(provider))
                                .ifPresent(_provider -> sessions
                                    .add(
                                        _provider
                                            .deserialize(data, registry)
                                            .doOnNext(session::register)
                                    ));
                        }

                        return Flux.merge(sessions)
                                   .then(Mono.just(session));

                    })
                    .doFinally((ignore) -> ReferenceCountUtil.safeRelease(buf));
            } catch (Throwable e) {
                return Mono.error(e);
            }
        }

        @Override
        public Mono<byte[]> serialize(PersistentSession session, DeviceRegistry registry) {
            MultiGatewayDeviceSession sessions = session.unwrap(MultiGatewayDeviceSession.class);
            return Flux
                .fromIterable(sessions)
                .filter(_session -> _session.isWrapFrom(PersistentSession.class))
                .flatMap(_session -> {
                    PersistentSession s = _session.unwrap(PersistentSession.class);
                    DeviceSessionProvider provider = DeviceSessionProviders.lookup(s.getProvider()).orElse(null);
                    if (provider != null) {
                        return provider
                            .serialize(s, registry)
                            .map(bytes -> Tuples.of(provider.getId(), bytes));
                    }
                    return Mono.empty();
                })
                .reduceWith(
                    () -> {
                        ByteBuf buf = ByteBufAllocator.DEFAULT.heapBuffer(1024);
                        byte[] deviceId = sessions.getDeviceId().getBytes();
                        buf.writeShort(deviceId.length);
                        buf.writeBytes(deviceId);
                        return buf;
                    },
                    (buf, tp2) -> {
                        byte[] provider = tp2.getT1().getBytes();
                        buf.writeShort(provider.length);
                        buf.writeBytes(provider);
                        buf.writeInt(tp2.getT2().length);
                        buf.writeBytes(tp2.getT2());
                        return buf;
                    })
                .map(buf -> {
                    try {
                        return ByteBufUtil.getBytes(buf);
                    } finally {
                        buf.release();
                    }
                });

        }
    }
}
