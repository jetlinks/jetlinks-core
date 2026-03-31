package org.jetlinks.core.server;

/**
 * 连接度量指标
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

    /**
     * 转换为度量指标信息对象
     *
     * @return 度量指标信息
     */
    default ConnectionMetricsInfo toInfo(){
        return new ConnectionMetricsInfo(
            getReadBytes(),
            getWrittenBytes(),
            getConnectTime(),
            getLastCommTime(),
            getTotalMessages(),
            getPendingMessages(),
            getDroppedMessages()
        );
    }
}
