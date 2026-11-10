package org.jetlinks.core.device.session;

import lombok.Getter;
import lombok.Setter;
import org.jetlinks.core.server.ConnectionMetricsInfo;
import org.jetlinks.core.server.ConnectionMetricsSupport;
import org.jetlinks.core.server.session.ClientConnectionSession;
import org.jetlinks.core.server.session.DeviceSession;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serial;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 当前会话的统计快照，不含连接身份、地址或历史，不维护常驻统计状态。
 * 节点读取时汇总一次，请求端按同一规则合并；覆盖仅针对本次已发现服务。
 * 远端节点只返回统计，覆盖由请求端记录实际调用结果，不能信任远端自报的覆盖。
 *
 * @see DeviceSessionManager#getDeviceSessionStatistics(String)
 * @since 1.3.3
 */
@Getter
@Setter
public class DeviceSessionStatistics implements Externalizable {
    @Serial
    private static final long serialVersionUID = 1L;
    // The version also fixes histogram boundaries/order; changing that schema requires a new version.
    private static final int VERSION = 1;

    private long sessionCount;
    /** 无连接明细能力的会话使连接数未知，成功无会话则为0。 */
    private Long connectionCount = 0L;
    /** 会话自身的时间，区别于 metrics 中连接实例的时间边界。 */
    private long sessionConnectTime;
    private long lastCommunicationTime;
    private ConnectionMetricsInfo metrics;
    private Set<String> requestedServerIds = Set.of();
    private Set<String> failedServerIds = Set.of();
    private boolean coverageKnown;

    /** Externalizable 和 JSON 编码器使用。 */
    public DeviceSessionStatistics() {
    }

    public boolean isComplete() {
        return coverageKnown && !requestedServerIds.isEmpty() && failedServerIds.isEmpty();
    }

    /** 在查询出口记录一次覆盖，不为各节点构造重复覆盖信封。 */
    public DeviceSessionStatistics withCoverage(Set<String> requested, Set<String> failed, boolean known) {
        requestedServerIds = Set.copyOf(requested);
        failedServerIds = Set.copyOf(failed);
        coverageKnown = known;
        return this;
    }

    /** 从真实会话直接读取连接指标，不生成 DeviceConnectionInfo 或访问地址、连接ID。 */
    public static DeviceSessionStatistics of(DeviceSession session) {
        DeviceSessionStatistics result = new DeviceSessionStatistics();
        if (session == null) return result;
        result.sessionCount = 1;
        result.sessionConnectTime = session.connectTime();
        result.lastCommunicationTime = session.lastPingTime();
        if (!session.isWrapFrom(ClientConnectionSession.class)) {
            result.connectionCount = null;
            return result;
        }
        var connections = List.copyOf(session.unwrap(ClientConnectionSession.class).getConnections());
        result.connectionCount = (long) connections.size();
        result.metrics = ConnectionMetricsInfo.aggregate(connections, connection -> {
            var metrics = connection.metrics();
            return metrics == null ? null : metrics.toInfo();
        });
        return result;
    }

    /** 为外部旧管理器适配已读取的明细，完整性沿用原读取结果，不能升级未知覆盖。 */
    public static DeviceSessionStatistics fromReadResult(DeviceSessionReadResult read) {
        return fromSessions(read.getSessions()).withCoverage(read.getRequestedServerIds(),
            read.getFailedServerIds(), read.isCoverageKnown());
    }

    public static DeviceSessionStatistics fromSessions(Iterable<DeviceSessionInfo> sessions) {
        List<DeviceSessionStatistics> values = new ArrayList<>();
        for (DeviceSessionInfo info : sessions) {
            var current = new DeviceSessionStatistics();
            current.sessionCount = 1;
            current.sessionConnectTime = info.getConnectTime();
            current.lastCommunicationTime = info.getLastCommTime();
            current.connectionCount = info.getConnections() == null ? null : (long) info.getConnections().size();
            current.metrics = info.getMetrics();
            values.add(current);
        }
        return aggregate(values);
    }

    /**
     * 只合并成功节点；空节点不遮蔽其他节点的指标，缺失能力保持未知。
     * 确认平均值由样本数和总耗时求得，P95合桶后计算，不平均节点的派生值。
     * 调用方负责补充本次请求覆盖，不合并各输入自报的覆盖。
     */
    public static DeviceSessionStatistics aggregate(Iterable<DeviceSessionStatistics> nodes) {
        var result = new DeviceSessionStatistics();
        List<DeviceSessionStatistics> connected = new ArrayList<>();
        for (DeviceSessionStatistics node : nodes) {
            if (node.sessionCount > 0) {
                result.sessionConnectTime = result.sessionCount == 0 ? node.sessionConnectTime
                    : Math.min(result.sessionConnectTime, node.sessionConnectTime);
                result.lastCommunicationTime = Math.max(result.lastCommunicationTime, node.lastCommunicationTime);
            }
            result.sessionCount += node.sessionCount;
            result.connectionCount = result.connectionCount == null || node.connectionCount == null ? null
                : result.connectionCount + node.connectionCount;
            if (node.connectionCount == null || node.connectionCount > 0) connected.add(node);
        }
        result.metrics = ConnectionMetricsInfo.aggregate(connected, DeviceSessionStatistics::getMetrics);
        return result;
    }

    /**
     * v1：位标记省略未知值、零值和空桶；仅写原始数值，不写类元数据或连接明细。
     * 平均值/P95通常由计数、总耗时和桶重建，仅为旧快照保留无原始事实的派生值。
     * 版本不匹配或截断必须失败，不能解码成一个成功的空统计。
     */
    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeByte(VERSION);
        int flags = (connectionCount != null ? 1 : 0) | (metrics != null ? 2 : 0) | (coverageKnown ? 4 : 0)
            | (sessionConnectTime != 0 ? 8 : 0) | (lastCommunicationTime != 0 ? 16 : 0);
        out.writeByte(flags);
        out.writeLong(sessionCount);
        if (connectionCount != null) out.writeLong(connectionCount);
        if (sessionConnectTime != 0) out.writeLong(sessionConnectTime);
        if (lastCommunicationTime != 0) out.writeLong(lastCommunicationTime);
        if (metrics != null) writeMetrics(metrics, out);
        writeServers(requestedServerIds, out);
        writeServers(failedServerIds, out);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException {
        if (in.readUnsignedByte() != VERSION) throw new IOException("unsupported session statistics version");
        int flags = in.readUnsignedByte();
        sessionCount = in.readLong();
        connectionCount = (flags & 1) != 0 ? Long.valueOf(in.readLong()) : null;
        sessionConnectTime = (flags & 8) != 0 ? in.readLong() : 0;
        lastCommunicationTime = (flags & 16) != 0 ? in.readLong() : 0;
        metrics = (flags & 2) != 0 ? readMetrics(in) : null;
        requestedServerIds = readServers(in);
        failedServerIds = readServers(in);
        coverageKnown = (flags & 4) != 0;
    }

    private static void writeServers(Set<String> servers, ObjectOutput out) throws IOException {
        out.writeInt(servers.size());
        for (String server : servers) out.writeUTF(server);
    }

    private static Set<String> readServers(ObjectInput in) throws IOException {
        int count = in.readInt();
        if (count < 0) throw new IOException("negative server count");
        if (count == 0) return Set.of();
        Set<String> result = new HashSet<>();
        for (int i = 0; i < count; i++) result.add(in.readUTF());
        return Set.copyOf(result);
    }

    private static void writeMetrics(ConnectionMetricsInfo m, ObjectOutput out) throws IOException {
        boolean average = (m.getAckCount() == null || m.getAckCount() <= 0 || m.getAckTotalDuration() == null)
            && m.getAckAverageDuration() != null;
        boolean percentile = m.getAckDurationBuckets() == null && m.getAckDurationP95() != null;
        int mask = (m.getReadBytes() != 0 ? 1 : 0) | (m.getWriteBytes() != 0 ? 1 << 1 : 0)
            | (m.getConnectTime() != 0 ? 1 << 2 : 0) | (m.getLastCommTime() != 0 ? 1 << 3 : 0)
            | (m.getTotalMessages() != 0 ? 1 << 4 : 0) | (m.getPendingMessages() != 0 ? 1 << 5 : 0)
            | (m.getDroppedMessages() != 0 ? 1 << 6 : 0)
            | (m.getLatestConnectTime() != m.getConnectTime() ? 1 << 7 : 0)
            | (m.getEarliestLastCommTime() != m.getLastCommTime() ? 1 << 8 : 0)
            | (m.getReadErrors() != null ? 1 << 9 : 0) | (m.getTransportErrors() != null ? 1 << 10 : 0)
            | (m.getWriteSuccesses() != null ? 1 << 11 : 0) | (m.getWriteErrors() != null ? 1 << 12 : 0)
            | (m.getAckCount() != null ? 1 << 13 : 0) | (m.getAckTimeouts() != null ? 1 << 14 : 0)
            | (m.getAckTotalDuration() != null ? 1 << 15 : 0) | (m.getAckMaxDuration() != null ? 1 << 16 : 0)
            | (m.getAckLastDuration() != null ? 1 << 17 : 0) | (m.getAckDurationObservationStart() != null ? 1 << 18 : 0)
            | (m.getAckDurationBuckets() != null ? 1 << 19 : 0) | (average ? 1 << 20 : 0) | (percentile ? 1 << 21 : 0);
        out.writeInt(mask);
        if ((mask & 1) != 0) out.writeLong(m.getReadBytes());
        if ((mask & 1 << 1) != 0) out.writeLong(m.getWriteBytes());
        if ((mask & 1 << 2) != 0) out.writeLong(m.getConnectTime());
        if ((mask & 1 << 3) != 0) out.writeLong(m.getLastCommTime());
        if ((mask & 1 << 4) != 0) out.writeLong(m.getTotalMessages());
        if ((mask & 1 << 5) != 0) out.writeLong(m.getPendingMessages());
        if ((mask & 1 << 6) != 0) out.writeLong(m.getDroppedMessages());
        if ((mask & 1 << 7) != 0) out.writeLong(m.getLatestConnectTime());
        if ((mask & 1 << 8) != 0) out.writeLong(m.getEarliestLastCommTime());
        if (m.getReadErrors() != null) out.writeLong(m.getReadErrors());
        if (m.getTransportErrors() != null) out.writeLong(m.getTransportErrors());
        if (m.getWriteSuccesses() != null) out.writeLong(m.getWriteSuccesses());
        if (m.getWriteErrors() != null) out.writeLong(m.getWriteErrors());
        if (m.getAckCount() != null) out.writeLong(m.getAckCount());
        if (m.getAckTimeouts() != null) out.writeLong(m.getAckTimeouts());
        if (m.getAckTotalDuration() != null) out.writeDouble(m.getAckTotalDuration());
        if (m.getAckMaxDuration() != null) out.writeDouble(m.getAckMaxDuration());
        if (m.getAckLastDuration() != null) out.writeDouble(m.getAckLastDuration());
        if (m.getAckDurationObservationStart() != null) out.writeLong(m.getAckDurationObservationStart());
        if (m.getAckDurationBuckets() != null) {
            long[] buckets = m.getAckDurationBuckets();
            if (buckets.length != ConnectionMetricsSupport.ACK_DURATION_BUCKET_COUNT) throw new IOException("invalid histogram length");
            int nonzero = 0;
            for (int i = 0; i < buckets.length; i++) if (buckets[i] != 0) nonzero |= 1 << i;
            out.writeShort(nonzero);
            for (int i = 0; i < buckets.length; i++) if ((nonzero & 1 << i) != 0) out.writeLong(buckets[i]);
        }
        if (average) out.writeDouble(m.getAckAverageDuration());
        if (percentile) out.writeDouble(m.getAckDurationP95());
    }

    private static ConnectionMetricsInfo readMetrics(ObjectInput in) throws IOException {
        var m = new ConnectionMetricsInfo();
        int mask = in.readInt();
        if ((mask & 1) != 0) m.setReadBytes(in.readLong());
        if ((mask & 1 << 1) != 0) m.setWriteBytes(in.readLong());
        if ((mask & 1 << 2) != 0) m.setConnectTime(in.readLong());
        if ((mask & 1 << 3) != 0) m.setLastCommTime(in.readLong());
        if ((mask & 1 << 4) != 0) m.setTotalMessages(in.readLong());
        if ((mask & 1 << 5) != 0) m.setPendingMessages(in.readLong());
        if ((mask & 1 << 6) != 0) m.setDroppedMessages(in.readLong());
        if ((mask & 1 << 7) != 0) m.setLatestConnectTime(in.readLong());
        if ((mask & 1 << 8) != 0) m.setEarliestLastCommTime(in.readLong());
        if ((mask & 1 << 9) != 0) m.setReadErrors(in.readLong());
        if ((mask & 1 << 10) != 0) m.setTransportErrors(in.readLong());
        if ((mask & 1 << 11) != 0) m.setWriteSuccesses(in.readLong());
        if ((mask & 1 << 12) != 0) m.setWriteErrors(in.readLong());
        if ((mask & 1 << 13) != 0) m.setAckCount(in.readLong());
        if ((mask & 1 << 14) != 0) m.setAckTimeouts(in.readLong());
        if ((mask & 1 << 15) != 0) m.setAckTotalDuration(in.readDouble());
        if ((mask & 1 << 16) != 0) m.setAckMaxDuration(in.readDouble());
        if ((mask & 1 << 17) != 0) m.setAckLastDuration(in.readDouble());
        if ((mask & 1 << 18) != 0) m.setAckDurationObservationStart(in.readLong());
        if ((mask & 1 << 19) != 0) {
            int nonzero = in.readUnsignedShort();
            if ((nonzero >>> ConnectionMetricsSupport.ACK_DURATION_BUCKET_COUNT) != 0) throw new IOException("invalid histogram mask");
            long[] buckets = new long[ConnectionMetricsSupport.ACK_DURATION_BUCKET_COUNT];
            for (int i = 0; i < buckets.length; i++) if ((nonzero & 1 << i) != 0) buckets[i] = in.readLong();
            m.setAckDurationBuckets(buckets);
        }
        if ((mask & 1 << 20) != 0) m.setAckAverageDuration(in.readDouble());
        if ((mask & 1 << 21) != 0) m.setAckDurationP95(in.readDouble());
        return m;
    }
}
