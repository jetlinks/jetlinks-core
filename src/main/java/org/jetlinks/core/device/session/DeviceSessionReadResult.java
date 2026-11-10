package org.jetlinks.core.device.session;

import lombok.Getter;

import java.util.List;
import java.util.Set;

/**
 * 一次指定设备的会话读取结果及节点覆盖信息，由查询端在本次订阅内组装。
 * 成功返回空会话与读取失败明确区分；完整只表示本次已发现的会话服务均成功响应，
 * 不保证集群拓扑发现完整或各节点处于同一时刻，也不保存跨请求状态。
 *
 * @see DeviceSessionManager#getDeviceSessionSnapshot(String)
 * @since 1.3.3
 */
@Getter
public final class DeviceSessionReadResult {
    private final List<DeviceSessionInfo> sessions;
    private final Set<String> requestedServerIds;
    private final Set<String> failedServerIds;
    private final boolean coverageKnown;

    /**
     * @param sessions 成功节点返回的快照；失败节点不得贡献部分会话
     * @param requestedServerIds 本次订阅开始时确定的节点集合，包含本地节点
     * @param failedServerIds 未成功完成读取的节点，必须属于请求集合
     * @param coverageKnown 是否能够证明请求覆盖；旧实现默认不能证明
     */
    public DeviceSessionReadResult(List<DeviceSessionInfo> sessions, Set<String> requestedServerIds,
                                   Set<String> failedServerIds, boolean coverageKnown) {
        this.sessions = List.copyOf(sessions);
        this.requestedServerIds = Set.copyOf(requestedServerIds);
        this.failedServerIds = Set.copyOf(failedServerIds);
        this.coverageKnown = coverageKnown;
        if (!this.requestedServerIds.containsAll(this.failedServerIds)) {
            throw new IllegalArgumentException("failed servers must belong to requested servers");
        }
    }

    /** 覆盖未知或未读取任何节点时，不将空结果解释成完整离线。 */
    public boolean isComplete() {
        return coverageKnown && !requestedServerIds.isEmpty() && failedServerIds.isEmpty();
    }

    /** 外部旧实现可以提供会话，但不能据此声称所有节点读取成功。 */
    public static DeviceSessionReadResult unknown(List<DeviceSessionInfo> sessions) {
        return new DeviceSessionReadResult(sessions, Set.of(), Set.of(), false);
    }
}
