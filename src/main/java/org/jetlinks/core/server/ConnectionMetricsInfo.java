package org.jetlinks.core.server;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Objects;
import java.util.function.Function;

/**
 * 单连接或同次读取中多个连接的度量快照。
 * 汇总只使用已有快照，不保存连接历史；各字段为弱一致读取。
 *
 * @author zhouhao
 * @since 1.2.1
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ConnectionMetricsInfo {

    /**
     * 总读取字节数
     */
    private long readBytes;

    /**
     * 总发送字节数
     */
    private long writeBytes;

    /**
     * 连接建立的时间戳(ms)
     */
    private long connectTime;

    /**
     * 最后一次通信的时间戳(ms)
     */
    private long lastCommTime;

    /** 汇总中最新的连接实例建立时间；单连接未单独设置时使用 connectTime。 */
    private Long latestConnectTime;

    /** 汇总中最早的最后通信时间；单连接未单独设置时使用 lastCommTime。 */
    private Long earliestLastCommTime;

    /**
     * 总消息吞吐量
     */
    private long totalMessages;

    /**
     * 待处理的消息数
     */
    private long pendingMessages;

    /**
     * 已丢弃的消息数
     */
    private long droppedMessages;

    /** 接收解码错误累计，未支持时为空。 */
    private Long readErrors;

    /** 未分类的传输异常累计，与读写失败不作为独立故障数相加。 */
    private Long transportErrors;

    /** 本地传输写入成功次数，不代表设备执行成功。 */
    private Long writeSuccesses;

    /** 本地传输写入失败次数。 */
    private Long writeErrors;

    /** 本连接已完成的对端确认次数，不含无确认语义的发送。 */
    private Long ackCount;

    /** 等待对端确认超时次数。 */
    private Long ackTimeouts;

    /** 成功确认累计耗时，单位秒；包含本地提交及对端处理。 */
    private Double ackTotalDuration;

    /** 成功确认平均耗时，单位秒。原始样本数和总耗时用于跨连接加权。 */
    private Double ackAverageDuration;

    /** 协议可提供的确认耗时P95；默认无分布时保持为空。 */
    private Double ackDurationP95;

    /** 成功确认最大耗时，单位秒。 */
    private Double ackMaxDuration;

    /** 最近一次成功确认耗时，单位秒；多个有确认样本的快照无法确定全局先后时为空。 */
    private Double ackLastDuration;

    /** 可选分布统计启用起点，epoch 毫秒；汇总时各分布起点不一致则为空。 */
    private Long ackDurationObservationStart;

    /** 成功确认耗时固定桶计数，读取时生成；无能力时为空。 */
    private long[] ackDurationBuckets;

    public Double getAckDurationP95() {
        return ackDurationBuckets == null ? ackDurationP95
            : ConnectionMetricsSupport.ackDurationPercentile(ackDurationBuckets, 0.95, ackMaxDuration);
    }

    public Double getAckAverageDuration() {
        return ackCount != null && ackCount > 0 && ackTotalDuration != null ? ackTotalDuration / ackCount : ackAverageDuration;
    }

    public Long getAckDurationSampleCount() {
        if (ackDurationBuckets == null) return null;
        long count = 0;
        for (long value : ackDurationBuckets) count += value;
        return count;
    }

    public long getLatestConnectTime() {
        return latestConnectTime == null ? connectTime : latestConnectTime;
    }

    public long getEarliestLastCommTime() {
        return earliestLastCommTime == null ? lastCommTime : earliestLastCommTime;
    }

    /**
     * 单次遍历汇总连接或会话快照，不再次访问连接，也不注册常驻监控状态。
     * 缺少任一快照时返回空值，缺少可选字段时仅保留仍完整的指标。
     * 确认耗时只使用支持确认的连接，按样本数加权；P95 必须先合桶。
     * 时间边界表示实例的新旧及通信差异，不代表物理重连次数或网络故障。
     *
     * @param snapshots 同次读取的快照集合
     * @param metrics 获取对应连接指标，不应产生 I/O 或修改源快照
     * @return 汇总快照；集合为空或存在缺失快照时返回 null
     * @since 1.3.3
     */
    public static <T> ConnectionMetricsInfo aggregate(Iterable<T> snapshots,
                                                     Function<? super T, ConnectionMetricsInfo> metrics) {
        ConnectionMetricsInfo result = null;
        boolean sampled = false;
        boolean completeDistribution = true;
        for (T snapshot : snapshots) {
            ConnectionMetricsInfo current = metrics.apply(snapshot);
            if (current == null) {
                return null;
            }
            if (result == null) {
                result = new ConnectionMetricsInfo(0, 0, current.connectTime, 0, 0, 0, 0);
                result.earliestLastCommTime = current.getEarliestLastCommTime();
                result.readErrors = 0L;
                result.transportErrors = 0L;
                result.writeSuccesses = 0L;
                result.writeErrors = 0L;
            }
            result.readBytes += current.readBytes;
            result.writeBytes += current.writeBytes;
            result.totalMessages += current.totalMessages;
            result.pendingMessages += current.pendingMessages;
            result.droppedMessages += current.droppedMessages;
            result.connectTime = Math.min(result.connectTime, current.connectTime);
            result.latestConnectTime = Math.max(result.getLatestConnectTime(), current.getLatestConnectTime());
            result.lastCommTime = Math.max(result.lastCommTime, current.lastCommTime);
            result.earliestLastCommTime = Math.min(result.getEarliestLastCommTime(), current.getEarliestLastCommTime());
            result.readErrors = sum(result.readErrors, current.readErrors);
            result.transportErrors = sum(result.transportErrors, current.transportErrors);
            result.writeSuccesses = sum(result.writeSuccesses, current.writeSuccesses);
            result.writeErrors = sum(result.writeErrors, current.writeErrors);

            if (current.ackCount == null) {
                continue;
            }
            if (result.ackCount == null) {
                result.ackCount = 0L;
                result.ackTimeouts = 0L;
            }
            result.ackCount += current.ackCount;
            result.ackTimeouts = sum(result.ackTimeouts, current.ackTimeouts);
            if (current.ackCount == 0) {
                continue;
            }
            result.ackTotalDuration = sampled ? sum(result.ackTotalDuration, current.ackTotalDuration) : current.ackTotalDuration;
            result.ackMaxDuration = sampled ? max(result.ackMaxDuration, current.ackMaxDuration) : current.ackMaxDuration;
            // 快照没有最近确认的时间戳，不能用最大耗时冒充全局最近一次。
            result.ackLastDuration = sampled ? null : current.ackLastDuration;
            // 不将不同连接的分布起点合成一个并不存在的共同采样窗口。
            if (!sampled) {
                result.ackDurationObservationStart = current.ackDurationObservationStart;
            } else if (!Objects.equals(result.ackDurationObservationStart, current.ackDurationObservationStart)) {
                result.ackDurationObservationStart = null;
            }
            sampled = true;
            if (!completeDistribution) {
                continue;
            }
            long[] buckets = current.ackDurationBuckets;
            if (buckets == null || buckets.length != ConnectionMetricsSupport.ACK_DURATION_BUCKET_COUNT) {
                completeDistribution = false;
                result.ackDurationBuckets = null;
                continue;
            }
            if (result.ackDurationBuckets == null) {
                result.ackDurationBuckets = new long[ConnectionMetricsSupport.ACK_DURATION_BUCKET_COUNT];
            }
            for (int i = 0; i < buckets.length; i++) {
                if (buckets[i] < 0 || Long.MAX_VALUE - result.ackDurationBuckets[i] < buckets[i]) {
                    completeDistribution = false;
                    result.ackDurationBuckets = null;
                    break;
                }
                result.ackDurationBuckets[i] += buckets[i];
            }
        }
        if (result != null && result.ackDurationBuckets == null) {
            result.ackDurationObservationStart = null;
        }
        return result;
    }

    private static Long sum(Long left, Long right) {
        return left == null || right == null ? null : left + right;
    }

    private static Double sum(Double left, Double right) {
        return left == null || right == null ? null : left + right;
    }

    private static Double max(Double left, Double right) {
        return left == null || right == null ? null : Math.max(left, right);
    }

    /** Keep the published seven-field constructor for existing protocol integrations. */
    public ConnectionMetricsInfo(long readBytes, long writeBytes, long connectTime, long lastCommTime,
                                 long totalMessages, long pendingMessages, long droppedMessages) {
        this.readBytes = readBytes;
        this.writeBytes = writeBytes;
        this.connectTime = connectTime;
        this.lastCommTime = lastCommTime;
        this.totalMessages = totalMessages;
        this.pendingMessages = pendingMessages;
        this.droppedMessages = droppedMessages;
    }

    /**
     * 保留新增时间边界字段前的完整构造器，兼容已编译的协议集成。
     */
    public ConnectionMetricsInfo(long readBytes, long writeBytes, long connectTime, long lastCommTime,
                                 long totalMessages, long pendingMessages, long droppedMessages,
                                 Long readErrors, Long transportErrors, Long writeSuccesses, Long writeErrors,
                                 Long ackCount, Long ackTimeouts, Double ackTotalDuration, Double ackAverageDuration,
                                 Double ackDurationP95, Double ackMaxDuration, Double ackLastDuration,
                                 Long ackDurationObservationStart, long[] ackDurationBuckets) {
        this(readBytes, writeBytes, connectTime, lastCommTime, totalMessages, pendingMessages, droppedMessages);
        this.readErrors = readErrors;
        this.transportErrors = transportErrors;
        this.writeSuccesses = writeSuccesses;
        this.writeErrors = writeErrors;
        this.ackCount = ackCount;
        this.ackTimeouts = ackTimeouts;
        this.ackTotalDuration = ackTotalDuration;
        this.ackAverageDuration = ackAverageDuration;
        this.ackDurationP95 = ackDurationP95;
        this.ackMaxDuration = ackMaxDuration;
        this.ackLastDuration = ackLastDuration;
        this.ackDurationObservationStart = ackDurationObservationStart;
        this.ackDurationBuckets = ackDurationBuckets;
    }

}
