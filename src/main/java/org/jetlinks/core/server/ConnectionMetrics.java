package org.jetlinks.core.server;

import io.swagger.v3.oas.annotations.media.Schema;
import org.jetlinks.core.annotation.metric.Counter;
import org.jetlinks.core.annotation.metric.Duration;
import org.jetlinks.core.annotation.metric.Gauge;
import org.jetlinks.core.annotation.metric.Timestamp;

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
    @Counter(unit = "B")
    @Schema(title = "接收字节", description = "当前观测实例累计接收的传输字节，不代表网卡物理流量")
    long getReadBytes();

    /**
     * 获取总发送字节数
     *
     * @return 总发送字节数
     */
    @Counter(unit = "B")
    @Schema(title = "发送字节", description = "当前观测实例累计提交给传输层的字节，不代表对端已处理")
    long getWrittenBytes();

    /**
     * 获取连接建立的时间戳(ms)
     *
     * @return 连接时间戳
     */
    @Timestamp
    @Schema(title = "连接建立时间", description = "最近成功建立连接的epoch毫秒时间，零表示尚未连接")
    long getConnectTime();

    /**
     * 获取最后一次通信的时间戳(ms)
     *
     * @return 最后一次通信时间戳
     */
    @Timestamp
    @Schema(title = "最近通信时间", description = "最近一次实际收发字节的epoch毫秒时间，零表示尚无通信")
    long getLastCommTime();

    /**
     * 获取总消息吞吐量
     *
     * @return 总消息数
     */
    @Counter
    @Schema(title = "接收消息", description = "累计完成解码的协议消息数量，TCP读取块不作为消息")
    long getTotalMessages();

    /**
     * 获取待处理的消息数
     *
     * @return 待处理消息数
     */
    @Gauge
    @Schema(title = "待完成请求", description = "已经提交但尚未结束的请求，包括传输等待及等待对端响应")
    long getPendingMessages();

    /**
     * 获取已丢弃的消息数
     *
     * @return 已丢弃消息数
     */
    @Counter
    @Schema(title = "丢弃消息", description = "传输或协议层明确丢弃的消息，不包含平台清洗忽略")
    long getDroppedMessages();

    /** 当前已成功建立且尚未关闭的物理连接数，未支持时为空。 */
    @Gauge
    @Schema(title = "活动连接", description = "当前观测资源所属的活动物理连接数，未支持时为空")
    default Long getActiveConnections() { return null; }

    /** 当前观测实例累计开始的物理建连次数，未支持时为空。 */
    @Counter
    @Schema(title = "连接尝试", description = "累计物理建连尝试数，不代表协议握手成功")
    default Long getConnectAttempts() { return null; }

    /** 当前观测实例累计失败的物理建连次数，未支持时为空。 */
    @Counter
    @Schema(title = "连接失败", description = "累计物理建连失败数，未支持时为空")
    default Long getConnectFailures() { return null; }

    /** 接收解码错误累计，未支持时为空。 */
    @Counter
    @Schema(title = "解码失败", description = "传输接收后的协议解码失败累计，未支持时为空")
    default Long getReadErrors() { return null; }

    /** 未分类的传输异常累计，与读写失败不作为独立故障数相加。 */
    @Counter
    @Schema(title = "传输错误", description = "底层传输异常累计，与请求失败可能重叠，不应直接相加")
    default Long getTransportErrors() { return null; }

    /** 本地传输写入成功次数，不代表设备执行成功。 */
    @Counter
    @Schema(title = "写入成功", description = "本地传输写入成功累计，不代表设备业务执行成功")
    default Long getWriteSuccesses() { return null; }

    /** 本地传输写入失败次数。 */
    @Counter
    @Schema(title = "写入失败", description = "本地传输写入失败累计，未支持时为空")
    default Long getWriteErrors() { return null; }

    /** 等待对端确认超时次数。 */
    @Counter
    @Schema(title = "响应超时", description = "已经提交的通信请求等待对端响应超时次数")
    default Long getAckTimeouts() { return null; }

    /** 协议请求失败累计，包括确认超时或拒绝；无请求确认语义时为空。 */
    @Counter
    @Schema(title = "请求失败", description = "通信请求失败累计，包含响应超时及协议异常，与传输错误可能重叠")
    default Long getRequestErrors() { return null; }

    /** 成功确认平均耗时，单位秒；无成功样本时为空。 */
    @Duration(unit = "seconds")
    @Schema(title = "平均响应耗时", description = "成功通信请求从本地提交到响应完成的平均耗时，包含本地等待；无样本时为空")
    default Double getAckAverageDuration() { return null; }

    /** 成功确认最大耗时，单位秒。 */
    @Duration(unit = "seconds")
    @Schema(title = "最大响应耗时", description = "当前观测实例成功通信请求的最大耗时，不是窗口峰值或网络RTT")
    default Double getAckMaxDuration() { return null; }

    /** 最近一次成功确认耗时，单位秒；没有成功样本时为空。 */
    @Duration(unit = "seconds")
    @Schema(title = "最近响应耗时", description = "最近一次成功通信请求的耗时，未完成成功请求时为空")
    default Double getAckLastDuration() { return null; }

    /** 固定桶估算的P95确认耗时上界，单位秒；分布默认关闭，无成功样本时为空。 */
    @Duration(unit = "seconds")
    @Schema(title = "响应耗时P95", description = "可选分布估计值，未启用分布统计时为空")
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
        info.setActiveConnections(getActiveConnections());
        info.setConnectAttempts(getConnectAttempts());
        info.setConnectFailures(getConnectFailures());
        info.setReadErrors(getReadErrors());
        info.setTransportErrors(getTransportErrors());
        info.setWriteSuccesses(getWriteSuccesses());
        info.setWriteErrors(getWriteErrors());
        info.setAckTimeouts(getAckTimeouts());
        info.setRequestErrors(getRequestErrors());
        info.setAckAverageDuration(getAckAverageDuration());
        info.setAckMaxDuration(getAckMaxDuration());
        info.setAckDurationP95(getAckDurationP95());
        info.setAckLastDuration(getAckLastDuration());
        return info;
    }
}
