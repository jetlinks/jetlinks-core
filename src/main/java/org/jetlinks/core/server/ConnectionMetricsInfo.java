package org.jetlinks.core.server;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 连接度量指标信息
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

    /** 最近一次成功确认耗时，单位秒。 */
    private Double ackLastDuration;

    /** 可选分布统计启用起点，epoch 毫秒。 */
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
}
