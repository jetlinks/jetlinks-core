package org.jetlinks.core.server.session;

import org.jetlinks.core.command.Command;
import org.jetlinks.core.device.DeviceOperator;
import org.jetlinks.core.message.DeviceMessage;
import org.jetlinks.core.message.codec.*;
import org.jetlinks.core.trace.DeviceTracer;
import org.jetlinks.core.trace.DeviceTracer.SpanName;
import org.jetlinks.core.trace.TraceHolder;
import org.jetlinks.core.utils.Reactors;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Optional;

import static org.jetlinks.core.trace.DeviceTracer.SpanName.*;
import static org.jetlinks.core.trace.FluxTracer.create;

/**
 * 设备会话,通常对应一个设备连接
 *
 * @author zhouhao
 * @see ChildrenDeviceSession
 * @see KeepOnlineSession
 * @since 1.0.0
 */
public interface DeviceSession {

    /**
     * @return 会话ID
     */
    String getId();

    /**
     * @return 设备ID
     */
    String getDeviceId();

    /**
     * 获取设备操作对象,在类似TCP首次请求的场景下,返回值可能为<code>null</code>.
     * 可以通过判断此返回值是否为<code>null</code>,来处理首次连接的情况。
     *
     * @return void
     */
    @Nullable
    DeviceOperator getOperator();

    /**
     * @return 最近心跳时间
     */
    long lastPingTime();

    /**
     * @return 创建时间
     */
    long connectTime();

    /**
     * 发送消息给会话
     *
     * @param encodedMessage 消息
     * @return 是否成功
     * @see org.jetlinks.core.message.codec.MqttMessage
     */
    Mono<Boolean> send(EncodedMessage encodedMessage);


    @Nonnull
    @Deprecated
    default <V> Mono<V> execute(@Nonnull Command<V> command) {
        return Mono.error(UnsupportedOperationException::new);
    }

    /**
     * 传输协议,比如MQTT,TCP等
     *
     * @return void
     */
    Transport getTransport();

    /**
     * 关闭session
     */
    void close();

    /**
     * 心跳
     *
     * @see DeviceSession#keepAlive()
     */
    @Deprecated
    void ping();

    /**
     * @return 会话是否存活
     */
    boolean isAlive();

    /**
     * 设置close回调
     *
     * @param call 回调
     */
    void onClose(Runnable call);

    /**
     * @return 会话连接的服务ID
     */
    @Deprecated
    default Optional<String> getServerId() {
        return Optional.empty();
    }

    /**
     * @return 客户端地址
     */
    default Optional<InetSocketAddress> getClientAddress() {
        return Optional.empty();
    }

    default void keepAlive() {
        ping();
    }

    /**
     * 设置心跳超时时间
     *
     * @param timeout 心跳超时时间
     */
    default void setKeepAliveTimeout(Duration timeout) {

    }

    default Duration getKeepAliveTimeout() {
        return Duration.ZERO;
    }

    /**
     * 判断会话是否包装自指定的类型,在某些场景下,可能会对会话进行包装
     *
     * @param type 类型
     * @return 是否包装自指定的类型
     */
    default boolean isWrapFrom(Class<?> type) {
        return type.isInstance(this);
    }

    /**
     * 展开为指定对会话类型,通过此方法拿到最原始对会话对象进行操作.如果类型不一致可能会抛出{@link ClassCastException}
     *
     * @param type 类型
     * @param <T>  类型泛型
     * @return 指定类型对会话
     */
    default <T extends DeviceSession> T unwrap(Class<T> type) {
        return type.cast(this);
    }

    /**
     * 异步判断session是否存活
     *
     * @return async result
     * @since 1.20
     */
    default Mono<Boolean> isAliveAsync() {
        return Mono.fromSupplier(this::isAlive);
    }

    /**
     * 判断会话当前会话与另外一个会话是否发生了变化
     *
     * @param another 另外一个会话
     * @return 是否发生变化
     */
    default boolean isChanged(DeviceSession another) {
        return !this.equals(another);
    }

    /**
     * 发送消息给设备,请勿在协议包的{@link DeviceMessageCodec#encode(MessageEncodeContext)}方法中调用此方法.
     *
     * @param context 消息上下文
     * @return 是否成功
     * @since 1.2.2
     */
    default Mono<Boolean> send(ToDeviceMessageContext context) {
        DeviceOperator device = context.getDevice();
        if (device == null) {
            return Reactors.ALWAYS_FALSE;
        }
        //获取设备使用的协议包,对消息进行编解码,然后发送到设备.
        return device
            .getProtocol()
            .flatMap(protocol -> protocol.getMessageCodec(getTransport()))
            .flatMapMany(codec -> codec.encode(context))
            .as(create(encode0(device.getDeviceId()),
                       (span, msg) -> span.setAttribute(DeviceTracer.SpanKey.message, msg.toString())))
            .as(context::sendToDevice);
    }

    static DeviceSession trace(DeviceSession target) {
        if (TraceHolder.isDisabled()) {
            return target;
        }
        return TraceDeviceSession.of(target);
    }
}
