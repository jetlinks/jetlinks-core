package org.jetlinks.core.server.session;

import org.jetlinks.core.message.codec.Transport;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Nullable;

/**
 * 设备会话管理器,用于管理所有设备连接会话
 *
 * @author zhouhao
 * @since 1.0.0
 */
public interface DeviceSessionManager {

    /**
     * 根据设备ID或者会话ID获取设备会话
     *
     * @param idOrDeviceId 设备ID或者会话ID
     * @return 设备会话, 不存在则返回<code>null</code>
     */
    @Nullable
    DeviceSession getSession(String idOrDeviceId);

    /**
     * 注册新到设备会话,如果已经存在相同设备ID到会话,将注销旧的会话.
     *
     * @param session 新的设备会话
     * @return 旧的设备会话, 不存在则返回<code>null</code>
     */
    @Nullable
    DeviceSession register(DeviceSession session);

    /**
     * 替换session
     *
     * @param oldSession 旧session
     * @param newSession 新session
     * @return 新session
     * @since 1.1.1
     */
   default DeviceSession replace(DeviceSession oldSession, DeviceSession newSession){
       return newSession;
   }

    /**
     * 使用会话ID或者设备ID注销设备会话
     *
     * @param idOrDeviceId 设备ID或者会话ID
     * @return 被注销的会话, 不存在则返回<code>null</code>
     */
    DeviceSession unregister(String idOrDeviceId);

    boolean sessionIsAlive(String deviceId);

    @Nullable
    ChildrenDeviceSession getSession(String deviceId, String childrenId);

    Mono<ChildrenDeviceSession> registerChildren(String deviceId, String childrenDeviceId);

    Mono<ChildrenDeviceSession> unRegisterChildren(String deviceId, String childrenId);

    Flux<DeviceSession> onRegister();

    Flux<DeviceSession> onUnRegister();

    Flux<DeviceSession> getAllSession();

    /**
     * 指定的协议是否已经超过了最大连接数量
     *
     * @param transport 协议
     * @return 是否超过
     */
    @Deprecated
    boolean isOutOfMaximumSessionLimit(Transport transport);

    /**
     * 获取指定协议的最大连接数量
     *
     * @param transport 协议
     * @return 最大连接数量
     */
    @Deprecated
    long getMaximumSession(Transport transport);

    /**
     * 获取指定协议的当前连接数量
     *
     * @param transport 协议
     * @return 当前连接数量
     */
    @Deprecated
    long getCurrentSession(Transport transport);

}
