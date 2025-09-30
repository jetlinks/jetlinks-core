package org.jetlinks.core.server.session;

import org.jetlinks.core.device.session.DeviceSessionInfo;
import org.jetlinks.core.message.DeviceMessage;
import org.jetlinks.core.message.Headers;
import org.jetlinks.core.server.ClientConnection;
import org.jetlinks.core.utils.HashUtils;
import reactor.core.Scannable;

import java.util.Comparator;
import java.util.function.BinaryOperator;
import java.util.function.Function;

public interface DeviceSessionSelector {

    /**
     * 任意一个
     */
    byte any = 0;

    /**
     * 所有会话
     */
    byte all = 1;

    /**
     * 发送给最近创建连接的会话
     */
    byte latest = 2;

    /**
     * 发送给最先创建连接的会话
     */
    byte oldest = 3;

    /**
     * 基于hash发送,相同的消息发送给相同的会话.
     * 默认根据消息类型,可通过header中自定义{@link org.jetlinks.core.message.Headers#routeKey}指定.
     *
     * @see org.jetlinks.core.message.Headers#routeKey
     */
    byte hashed = 4;

    /**
     * 最小负载
     */
    byte minimumLoad = 5;


    BinaryOperator<DeviceSession> SESSION_MINIMUM_LOAD = BinaryOperator
        .minBy(
            Comparator
                .comparingLong(e -> Scannable
                    .from(e).scanOrDefault(Scannable.Attr.BUFFERED, 0)));

    BinaryOperator<DeviceSession> SESSION_OLDEST = BinaryOperator
        .minBy(Comparator.comparingLong(DeviceSession::connectTime));

    BinaryOperator<DeviceSession> SESSION_LATEST = BinaryOperator
        .maxBy(Comparator.comparingLong(DeviceSession::connectTime));


    BinaryOperator<ClientConnection> CONNECTION_MINIMUM_LOAD = BinaryOperator
        .minBy(
            Comparator
                .comparingLong(c -> c
                    .scanOrDefault(Scannable.Attr.BUFFERED, 0)));

    @SuppressWarnings("unchecked")
    static <T extends ClientConnection> BinaryOperator<T> connectionMinimumLoad() {
        return (BinaryOperator<T>) CONNECTION_MINIMUM_LOAD;
    }


    BinaryOperator<ClientConnection> CONNECTION_OLDEST = BinaryOperator
        .minBy(Comparator.comparingLong(ClientConnection::connectTime));

    @SuppressWarnings("unchecked")
    static <T extends ClientConnection> BinaryOperator<T> connectionOldest() {
        return (BinaryOperator<T>)  CONNECTION_OLDEST;
    }

    BinaryOperator<ClientConnection> CONNECTION_LATEST = BinaryOperator
        .maxBy(Comparator.comparingLong(ClientConnection::connectTime));

    @SuppressWarnings("unchecked")
    static <T extends ClientConnection> BinaryOperator<T> connectionLatest() {
        return (BinaryOperator<T>)  CONNECTION_LATEST;
    }


    BinaryOperator<DeviceSessionInfo> SESSION_INFO_MINIMUM_LOAD = BinaryOperator
        .minBy(
            Comparator
                .comparingLong(DeviceSessionInfo::getPendingMessages));

    BinaryOperator<DeviceSessionInfo> SESSION_INFO_OLDEST = BinaryOperator
        .minBy(Comparator.comparingLong(DeviceSessionInfo::getConnectTime));

    BinaryOperator<DeviceSessionInfo> SESSION_INFO_LATEST = BinaryOperator
        .maxBy(Comparator.comparingLong(DeviceSessionInfo::getConnectTime));


    static <T> BinaryOperator<T> hashedOperator(Function<T, Object> keyMapper, DeviceMessage message) {
        Object route = message
            .getHeader(Headers.routeKey)
            .orElse(message.getMessageType());

        return (a, b) -> {
            Object aKey = keyMapper.apply(a);
            Object bKey = keyMapper.apply(b);

            return HashUtils
                .murmur3_128(aKey, route) > HashUtils
                .murmur3_128(bKey, route) ? a : b;
        };
    }

}
