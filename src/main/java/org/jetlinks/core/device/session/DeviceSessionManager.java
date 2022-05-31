package org.jetlinks.core.device.session;

import org.jetlinks.core.server.session.DeviceSession;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.function.Function;

/**
 * 设备会话管理器.
 * <p>
 * 设备会话可以存在于集群多台节点中,在设备注册注销时,会发送事件{@link DeviceSessionEvent},
 * 可通过监听事件{@link DeviceSessionManager#listenEvent(Function)}
 * 并根据事件中的{@link  DeviceSessionEvent#getType()}和{@link DeviceSessionEvent#isClusterExists()}值来进行
 * 自定义的业务处理.
 *
 * <pre>{@code
 *  sessionManager.listenEvent(event->{
 *      if(event.isClusterExists()){ //集群中依旧存在会话则忽略
 *          return Mono.empty();
 *      }
 *      return handleEvent(event); //处理事件
 *  });
 *
 * }</pre>
 *
 * @author zhouhao
 * @since 1.2
 */
public interface DeviceSessionManager {

    /**
     * 当前集群节点ID
     *
     * @return 集群节点ID
     */
    String getCurrentServerId();

    /**
     * 计算设备会话,通常用于注册和变更会话信息.
     * <p>
     * 如果之前会话不存在,执行后返回了新的会话,则将触发{@link  DeviceSessionEvent}.
     *
     * <pre>{@code
     *
     *  manager.compute(deviceId,old -> {
     *
     *   return old
     *          //会话已存在则替换为新的会话
     *          .map(this::replaceSession)
     *          //会话不存在则创建新的会话
     *          .switchIfEmpty(createNewSession());
     *  })
     *
     * }</pre>
     *
     * @param deviceId 设备ID
     * @param computer 会话处理器
     * @return 处理后的会话
     * @see DeviceSessionEvent.Type#register
     */
    Mono<DeviceSession> compute(String deviceId,
                                Function<Mono<DeviceSession>, Mono<DeviceSession>> computer);

    /**
     * 获取设备会话.会话不存在则返回{@link  Mono#empty()}.
     * <p>
     * 此方法仅会返回本地存活的会话信息.
     *
     * @param deviceId 设备ID
     * @return 会话
     */
    Mono<DeviceSession> getSession(String deviceId);

    /**
     * 获取当前服务节点的全部会话信息
     *
     * @return 会话
     */
    Flux<DeviceSession> getSessions();

    /**
     * 移除会话,如果会话存在将触发{@link DeviceSessionEvent}
     * <p>
     * 当设置参数{@code onlyLocal}为true时,将移除整个集群的会话.
     *
     * @param deviceId  设备ID
     * @param onlyLocal 是否只移除本地的会话信息
     * @return 有多少会话被移除
     */
    Mono<Long> remove(String deviceId, boolean onlyLocal);


    /**
     * 判断会话是否存活，包括本地和集群中的会话
     *
     * @param deviceId 设备ID
     * @return 是否存活
     */
    default Mono<Boolean> isAlive(String deviceId) {
        return isAlive(deviceId, false);
    }

    /**
     * 判断会话是否存活
     *
     * @param deviceId  设备ID
     * @param onlyLocal 是否仅判断本地的会话
     * @return 是否存活
     */
    Mono<Boolean> isAlive(String deviceId, boolean onlyLocal);

    /**
     * 获取会话总数
     *
     * @param onlyLocal 是否仅获取本地的会话数量
     * @return 总数
     */
    Mono<Long> totalSessions(boolean onlyLocal);

    /**
     * 监听并处理会话事件,可通过调用返回值{@link  Disposable#dispose()}来取消监听
     *
     * @param handler 事件处理器
     * @return Disposable
     */
    Disposable listenEvent(Function<DeviceSessionEvent, Mono<Void>> handler);


}
