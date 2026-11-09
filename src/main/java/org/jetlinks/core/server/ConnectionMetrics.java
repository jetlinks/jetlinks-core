package org.jetlinks.core.server;

/**
 * 连接度量指标。保留流量、背压、通信失败和对端确认耗时；速率、错误率及评分由采样方计算。
 * 可选能力未实现时返回空值，与已支持但计数为零区分。各字段为弱一致读取。
 *
 * @author zhouhao
 * @since 1.2.1
 */
public interface ConnectionMetrics {

    /**
     * 获取总读取字节数
     *
     * @return 总读取字节数
     */
    long getReadBytes();

    /**
     * 获取总发送字节数
     *
     * @return 总发送字节数
     */
    long getWrittenBytes();

    /**
     * 获取连接建立的时间戳(ms)
     *
     * @return 连接时间戳
     */
    long getConnectTime();

    /**
     * 获取最后一次通信的时间戳(ms)
     *
     * @return 最后一次通信时间戳
     */
    long getLastCommTime();

    /**
     * 获取总消息吞吐量
     *
     * @return 总消息数
     */
    long getTotalMessages();

    /**
     * 获取待处理的消息数
     *
     * @return 待处理消息数
     */
    long getPendingMessages();

    /**
     * 获取已丢弃的消息数
     *
     * @return 已丢弃消息数
     */
    long getDroppedMessages();

    /** 接收解码错误累计，未支持时为空。 */
    default Long getReadErrors() { return null; }

    /** 未分类的传输异常累计，与读写失败不作为独立故障数相加。 */
    default Long getTransportErrors() { return null; }

    /** 本地传输写入成功次数，不代表设备执行成功。 */
    default Long getWriteSuccesses() { return null; }

    /** 本地传输写入失败次数。 */
    default Long getWriteErrors() { return null; }

    /** 等待对端确认超时次数。 */
    default Long getAckTimeouts() { return null; }

    /** 成功确认平均耗时，单位秒；无成功样本时为空。 */
    default Double getAckAverageDuration() { return null; }

    /** 成功确认最大耗时，单位秒。 */
    default Double getAckMaxDuration() { return null; }

    /** 最近一次成功确认耗时，单位秒；没有成功样本时为空。 */
    default Double getAckLastDuration() { return null; }

    /** 固定桶估算的P95确认耗时上界，单位秒；分布默认关闭，无成功样本时为空。 */
    default Double getAckDurationP95() {
        return null;
    }

    /**
     * 转换为度量指标信息对象
     *
     * @return 度量指标信息
     */
    default ConnectionMetricsInfo toInfo(){
        ConnectionMetricsInfo info = new ConnectionMetricsInfo(
            getReadBytes(),
            getWrittenBytes(),
            getConnectTime(),
            getLastCommTime(),
            getTotalMessages(),
            getPendingMessages(),
            getDroppedMessages()
        );
        info.setReadErrors(getReadErrors());
        info.setTransportErrors(getTransportErrors());
        info.setWriteSuccesses(getWriteSuccesses());
        info.setWriteErrors(getWriteErrors());
        info.setAckTimeouts(getAckTimeouts());
        info.setAckAverageDuration(getAckAverageDuration());
        info.setAckMaxDuration(getAckMaxDuration());
        info.setAckDurationP95(getAckDurationP95());
        info.setAckLastDuration(getAckLastDuration());
        return info;
    }
}
