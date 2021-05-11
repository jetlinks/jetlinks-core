package org.jetlinks.core.server.session;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 响应式设备会话管理器
 *
 * @author zhouhao
 * @since 1.1.6
 */
public interface ReactiveDeviceSessionManager {

    /**
     * 获取会话,如果会话不存在或者已失效则返回{@link Mono#empty()}
     *
     * @param sessionIdOrDeviceId 会话ID或者设备ID
     * @return 会话
     */
    Mono<DeviceSession> getSession(String sessionIdOrDeviceId);

    /**
     * 注册会话,并返回旧的会话
     * @param session session
     * @return 旧的会话
     */
    Mono<DeviceSession> register(DeviceSession session);

    Mono<DeviceSession> unregister(String sessionIdOrDeviceId);

    Mono<DeviceSession> replace(DeviceSession session);

    Flux<DeviceSession> onRegister();

    Flux<DeviceSession> onUnRegister();

    Flux<DeviceSession> getAllSession();

    Mono<Long> totalSession();
}
