package org.jetlinks.core.message.codec;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;

/**
 * 传输协议定义,如: TCP,MQTT等,通常使用枚举来定义
 *
 * @author zhouhao
 * @since 1.0
 */
public interface Transport {

    /**
     * @return 唯一标识
     */
    String getId();

    /**
     * @return 名称，默认和ID一致
     */
    default String getName() {
        return getId();
    }

    /**
     * @return 描述
     */
    default String getDescription() {
        return null;
    }

    /**
     * 比较与指定等协议是否为同一种协议
     *
     * @param transport 要比较等协议
     * @return 是否为同一个协议
     */
    default boolean isSame(Transport transport) {
        return this == transport || this.getId().equals(transport.getId());
    }

    /**
     * 使用ID进行对比，判断是否为同一个协议
     *
     * @param transportId ID
     * @return 是否为同一个协议
     */
    default boolean isSame(String transportId) {
        return this.getId().equals(transportId);
    }

    /**
     * 使用指定的ID来创建协议定义
     *
     * @param id ID
     * @return Transport
     */
    static Transport of(String id) {
        return lookup(id).orElseGet(() -> (Transport & Serializable) () -> id);
    }

    /**
     * 通过ID查找协议定义,可通过{@link Transports#register(Transport)}来注册自定义的协议
     *
     * @param id ID
     * @return Optional
     * @since 1.1.6
     */
    static Optional<Transport> lookup(String id) {
        return Transports.lookup(id);
    }

    /**
     * @return 获取全部协议定义
     * @since 1.1.6
     */
    static List<Transport> getAll() {
        return Transports.get();
    }
}
