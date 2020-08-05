package org.jetlinks.core.message.codec.context;

import org.jetlinks.core.message.DeviceMessageReply;
import org.jetlinks.core.message.RepayableDeviceMessage;

import java.time.Duration;
import java.util.Optional;

/**
 * 编解码上下文
 *
 * @author zhouhao
 * @since 1.1.1
 */
public interface CodecContext {

    static CodecContext newContext() {
        return new CacheCodecContext();
    }

    /**
     * 缓存下行消息
     *
     * @param key     key
     * @param message 下行消息
     * @param ttl     有效期
     */
    void cacheDownstream(Object key, RepayableDeviceMessage<? extends DeviceMessageReply> message, Duration ttl);

    default void cacheDownstream(Object key, RepayableDeviceMessage<? extends DeviceMessageReply> message) {
        cacheDownstream(key, message, Duration.ofSeconds(30));
    }

    /**
     * 根据key获取下行消息,可通过下行消息来构造消息回复
     *
     * @param key    key
     * @param remove 自动删除
     * @param <T>    下行消息类型
     * @return 下行消息
     * @see RepayableDeviceMessage#newReply()
     */
    <T extends RepayableDeviceMessage<? extends DeviceMessageReply>> Optional<T> getDownstream(Object key, boolean remove);

    default <T extends RepayableDeviceMessage<? extends DeviceMessageReply>> Optional<T> removeDownstream(Object key) {
        return getDownstream(key, true);
    }


}
