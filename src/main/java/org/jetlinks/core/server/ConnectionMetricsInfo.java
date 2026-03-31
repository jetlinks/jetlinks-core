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
}
