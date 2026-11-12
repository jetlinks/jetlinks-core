package org.jetlinks.core.monitor.metrics;

import io.swagger.v3.oas.annotations.media.Schema;
import org.jetlinks.core.annotation.Attr;
import org.jetlinks.core.annotation.Expands;
import org.jetlinks.core.annotation.metric.Gauge;
import org.jetlinks.core.annotation.metric.Timestamp;
import org.jetlinks.core.annotation.metric.WindowMaximum;

/**
 * 一次固定分段峰值快照；起止时刻来自配置的时长/偏移，结束边界不包含在分段中。
 * available=false 时 maximum/occurredAtMillis 均为占位。值的业务单位由使用方绑定。
 *
 * @since 1.3.3
 * @see WindowMax.Snapshot
 */
public interface WindowMaxMetrics {
    @Timestamp
    @Schema(title = "分段开始", description = "当前固定分段的起始时刻，epoch毫秒，包含此边界")
    long getStartMillis();

    @Timestamp
    @Schema(title = "分段结束", description = "当前固定分段的结束时刻，epoch毫秒，不包含此边界")
    long getEndMillis();

    @Gauge
    @Schema(title = "分段有样本", description = "当前分段是否存在观测；过期或尚无样本时为false")
    boolean isAvailable();

    @WindowMaximum
    @Schema(title = "分段峰值", description = "固定分段内的最大值；仅available为true时有效，不是滚动窗口")
    long getMaximum();

    @Timestamp
    @Expands(key = "metric", value = {
        @Attr(key = "availableProperty", value = "available"),
        @Attr(key = "valueProperty", value = "maximum")
    })
    @Schema(title = "峰值发生时刻", description = "峰值首次出现的epoch毫秒；仅available为true时有效")
    long getOccurredAtMillis();
}
