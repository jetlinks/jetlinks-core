package org.jetlinks.core.device.session;

import org.jetlinks.core.server.session.DeviceSession;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Function;
import java.util.function.Predicate;

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
    Mono<DeviceSession> compute(@Nonnull String deviceId,
                                @Nonnull Function<Mono<DeviceSession>, Mono<DeviceSession>> computer);

    /**
     * 计算设备会话,用于处理创建会话或者更新会话.
     * 会话如果不存在则执行creator逻辑,如果会话已存在则执行updater逻辑.
     * <p>
     * updater如果执行后返回empty,则将注销会话.
     *
     * <pre>{@code
     *
     *  manager
     *  .compute(deviceId,createNewSession(),this::updateSession)
     *  .flatMap(this::handleSession)
     *
     * }</pre>
     *
     * @param deviceId 设备ID
     * @param creator  会话创建器,如果为null则不执行创建
     * @param updater  会话更新器,如果为null则不进行会话更新
     * @return 会话信息
     */
    Mono<DeviceSession> compute(@Nonnull String deviceId,
                                @Nullable Mono<DeviceSession> creator,
                                @Nullable Function<DeviceSession, Mono<DeviceSession>> updater);

    /**
     * 获取设备会话.会话不存在或者会话失效则返回{@link  Mono#empty()}.
     * <p>
     * 此方法仅会返回本地存活的会话信息.
     * 如果设备已经失效,将会触发离线事件{@link DeviceSessionEvent}
     *
     * @param deviceId 设备ID
     * @return 会话
     * @see DeviceSessionManager#getSession(String, boolean)
     */
    Mono<DeviceSession> getSession(String deviceId);

    /**
     * 获取设备会话.会话不存在则返回{@link Mono#empty()}.
     *
     * @param deviceId               设备ID
     * @param unregisterWhenNotAlive 当会话失效时,是否注销会话
     * @return 会话信息
     */
    Mono<DeviceSession> getSession(String deviceId, boolean unregisterWhenNotAlive);

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
     * 根据自定义判断逻辑来移除当前服务节点的会话
     *
     * @param deviceId  设备ID
     * @param predicate 判断逻辑
     * @return 有多少会话被移除 0 or 1
     * @since 1.2.3
     */
    Mono<Long> remove(String deviceId, Predicate<DeviceSession> predicate);

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
     * 检查会话是否存活,如果存活将同步连接信息到缓存中.
     *
     * @param deviceId  设备ID
     * @param onlyLocal 是否只检查本地会话
     * @return 是否存活
     */
    Mono<Boolean> checkAlive(String deviceId, boolean onlyLocal);

    /**
     * 获取会话总数
     *
     * @param onlyLocal 是否仅获取本地的会话数量
     * @return 总数
     */
    Mono<Long> totalSessions(boolean onlyLocal);

    /**
     * 获取全部会话信息
     *
     * @return 会话信息
     */
    Flux<DeviceSessionInfo> getSessionInfo();

    /**
     * 获取指定设备的会话信息
     *
     * @return 会话信息
     */
    Flux<DeviceSessionInfo> getDeviceSessionInfo(String deviceId);

    /**
     * 读取指定设备的本地及集群会话，并报告本次已发现节点的读取覆盖情况。
     * 每次订阅重新读取；成功但无会话与节点失败必须分开，失败节点不贡献部分数据。
     * 完整性只相对于本次服务发现快照，不是跨节点事务快照。沿用调用方的 RPC 超时，
     * 取消订阅应停止仍在进行的读取；实现不得将节点失败伪装为成功空结果。
     * 默认适配外部旧实现，但覆盖标为未知；需要完整性保证的调用方必须检查 isComplete。
     *
     * @param deviceId 设备标识，空白时实现可返回 Mono.empty()
     * @return 本次会话和覆盖结果；无法建立读取时可传播异常，不应静默重试
     * @see DeviceSessionReadResult#isComplete()
     * @since 1.3.3
     */
    default Mono<DeviceSessionReadResult> getDeviceSessionSnapshot(String deviceId) {
        return getDeviceSessionInfo(deviceId).collectList().map(DeviceSessionReadResult::unknown);
    }

    /**
     * 按需读取当前统计；集群实现应在各节点汇总并仅传输统计，避免传回连接明细。
     * 成功无会话返回计数0的快照，缺少能力保持未知；节点失败不能贡献部分数据。
     * 调用方检查 isComplete，完整性只相对于本次已发现服务，不是事务快照。
     * 默认从旧快照适配，保留其未知覆盖；取消与超时沿用现有读取链路。
     *
     * @param deviceId 设备ID，空白可返回 Mono.empty()
     * @return 本次统计及节点覆盖，不持久化或主动探测设备
     * @since 1.3.3
     */
    default Mono<DeviceSessionStatistics> getDeviceSessionStatistics(String deviceId) {
        return getDeviceSessionSnapshot(deviceId).map(DeviceSessionStatistics::fromReadResult);
    }

    /**
     * 获取指定服务的会话信息
     *
     * @param serverId 服务ID
     * @return 会话信息
     */
    Flux<DeviceSessionInfo> getSessionInfo(String serverId);

    /**
     * 获取本地的会话信息
     *
     * @return 会话信息
     */
    Flux<DeviceSessionInfo> getLocalSessionInfo();

    /**
     * 监听并处理会话事件,可通过调用返回值{@link  Disposable#dispose()}来取消监听
     *
     * @param handler 事件处理器
     * @return Disposable
     */
    Disposable listenEvent(Function<DeviceSessionEvent, Mono<Void>> handler);


}
