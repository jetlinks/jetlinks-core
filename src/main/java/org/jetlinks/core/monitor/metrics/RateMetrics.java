package org.jetlinks.core.monitor.metrics;

import io.swagger.v3.oas.annotations.media.Schema;
import org.jetlinks.core.annotation.metric.Counter;
import org.jetlinks.core.annotation.metric.EwmaRate;
import org.jetlinks.core.annotation.metric.Rate;
import org.jetlinks.core.annotation.metric.Statistic;

/**
 * 累计量与每秒速率的共享只读契约，具体数量/字节等单位由业务子契约绑定。
 * 读取各字段为弱一致；EWMA 使用五秒惰性节拍，首次节拍前为零，不表示精确窗口平均。
 *
 * @since 1.3.3
 * @see RateMeter
 */
public interface RateMetrics {
    @Counter(unit = "")
    @Statistic(MetricStatistic.count)
    @Schema(title = "累计量", description = "当前实例创建以来记录的总量，业务单位由调用方声明")
    long getCount();

    @Rate
    @Schema(title = "平均速率", description = "实例创建以来的平均每秒速率，尚无时间跨度时为零")
    double getMeanRate();

    @EwmaRate(value = 60, tickSeconds = 5)
    @Schema(title = "1分钟平滑速率", description = "时间常数60秒的每秒EWMA，五秒惰性推进，首次节拍前为零")
    double getOneMinuteRate();

    @EwmaRate(value = 300, tickSeconds = 5)
    @Schema(title = "5分钟平滑速率", description = "时间常数300秒的每秒EWMA，不是最近5分钟的精确平均")
    double getFiveMinuteRate();

    @EwmaRate(value = 900, tickSeconds = 5)
    @Schema(title = "15分钟平滑速率", description = "时间常数900秒的每秒EWMA，不是最近15分钟的精确平均")
    double getFifteenMinuteRate();
}
