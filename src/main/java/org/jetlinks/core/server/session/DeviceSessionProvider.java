package org.jetlinks.core.server.session;

import org.jetlinks.core.device.DeviceRegistry;
import reactor.core.publisher.Mono;

import java.util.Optional;

/**
 * 设备会话提供者
 *
 * @author zhouhao
 * @since 1.1.6
 */
public interface DeviceSessionProvider {

    /**
     * @return 提供者ID
     */
    String getId();

    /**
     * 反序列化会话
     *
     * @param sessionData 会话数据
     * @param registry    注册中心
     * @return 会话
     */
    Mono<PersistentSession> deserialize(byte[] sessionData, DeviceRegistry registry);

    /**
     * 序列化会话
     *
     * @param session  会话
     * @param registry 注册中心
     * @return 序列化后的数据
     */
    Mono<byte[]> serialize(PersistentSession session, DeviceRegistry registry);

    /**
     * 根据id获取Provider
     *
     * @param id ID
     * @return Provider
     * @see DeviceSessionProvider#getId()
     */
    static Optional<DeviceSessionProvider> lookup(String id) {
        return DeviceSessionProviders.lookup(id);
    }
}
