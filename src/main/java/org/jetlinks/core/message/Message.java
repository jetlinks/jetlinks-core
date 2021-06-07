package org.jetlinks.core.message;

import com.alibaba.fastjson.parser.ParserConfig;
import com.alibaba.fastjson.util.TypeUtils;
import org.jetlinks.core.metadata.Jsonable;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.Map;
import java.util.Optional;

import static org.jetlinks.core.message.MessageType.UNKNOWN;

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
 */
public interface Message extends Jsonable, Serializable {

    default MessageType getMessageType() {
        return UNKNOWN;
    }

    /**
     * 消息的唯一标识,用于在请求响应模式下对请求和响应进行关联.
     * <p>
     * 注意: 此消息ID为全系统唯一. 但是在很多情况下,设备可能不支持此类型的消息ID,
     * 此时需要在协议包中做好映射关系,比如使用:{@link java.util.concurrent.ConcurrentHashMap}进行消息绑定.
     * 还可以使用工具类:{@link org.jetlinks.core.message.codec.context.CodecContext}来进行此操作.
     *
     * @return 消息ID
     */
    String getMessageId();

    /**
     * @return 毫秒时间戳
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
    Message addHeader(String header, Object value);

    /**
     * 添加header,如果header已存在则放弃
     *
     * @param header header key
     * @param value  header 值
     * @return this
     */
    Message addHeaderIfAbsent(String header, Object value);


    /**
     * 删除一个header
     *
     * @param header header
     * @return this
     * @see Headers
     */
    Message removeHeader(String header);

    /**
     * @see Headers
     * @see Message#addHeader(String, Object)
     */
    default <T> Message addHeader(HeaderKey<T> header, T value) {
        return addHeader(header.getKey(), value);
    }

    /**
     * @see Headers
     * @see Message#addHeaderIfAbsent(String, Object)
     */
    default <T> Message addHeaderIfAbsent(HeaderKey<T> header, T value) {
        return addHeaderIfAbsent(header.getKey(), value);
    }

    @SuppressWarnings("all")
    default <T> Optional<T> getHeader(HeaderKey<T> key) {
        return getHeader(key.getKey())
                .map(v -> TypeUtils.cast(v, key.getType(), ParserConfig.global));
    }

    default <T> T getHeaderOrDefault(HeaderKey<T> key) {
        return getHeader(key).orElseGet(key::getDefaultValue);
    }

    default Optional<Object> getHeader(String header) {
        return Optional.ofNullable(getHeaders())
                       .map(headers -> headers.get(header));
    }

    default void validate(){

    }
}
