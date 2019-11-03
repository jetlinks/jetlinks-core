package org.jetlinks.core.message;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.Map;
import java.util.Optional;

/**
 * 设备消息
 *
 * @see org.jetlinks.core.message.property.ReadPropertyMessage
 * @see org.jetlinks.core.message.property.ReadPropertyMessageReply
 * @see org.jetlinks.core.message.property.WritePropertyMessage
 * @see org.jetlinks.core.message.property.WritePropertyMessageReply
 * @see org.jetlinks.core.message.function.FunctionInvokeMessage
 * @see org.jetlinks.core.message.function.FunctionInvokeMessageReply
 * @see org.jetlinks.core.message.event.EventMessage
 * @see DeviceOnlineMessage
 * @see DeviceOfflineMessage
 * @see ChildDeviceMessage
 * @see ChildDeviceMessageReply
 * @see ChildDeviceOfflineMessage
 * @see ChildDeviceOnlineMessage
 */
public interface Message extends Serializable {

    /**
     * @return 消息ID
     */
    String getMessageId();

    /**
     * @return 时间戳
     * @see System#currentTimeMillis()
     */
    long getTimestamp();

    /**
     * 消息头,用于自定义一些消息行为, 默认的一些消息头请看:{@link Headers}
     *
     * @return headers or null
     * @see Headers
     */
    @Nullable
    Map<String, Object> getHeaders();

    /**
     * 添加一个header
     *
     * @param header header
     * @param value  value
     * @return this
     * @see Headers
     */
    DeviceMessage addHeader(String header, Object value);

    /**
     * 删除一个header
     *
     * @param header header
     * @return this
     * @see Headers
     */
    DeviceMessage removeHeader(String header);

    /**
     * @see Headers
     * @see this#addHeader(String, Object)
     */
    default <T> DeviceMessage addHeader(HeaderKey<T> header, Object value) {
        return addHeader(header.getKey(), value);
    }

    @SuppressWarnings("all")
    default <T> Optional<T> getHeader(HeaderKey<T> key) {
        return getHeader(key.getKey())
                .map(v -> (T) v);
    }

    default Optional<Object> getHeader(String header) {
        return Optional.ofNullable(getHeaders())
                .map(headers -> headers.get(header));
    }
}
