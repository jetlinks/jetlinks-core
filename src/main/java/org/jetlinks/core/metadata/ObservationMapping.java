package org.jetlinks.core.metadata;

/**
 * 说明一个数值应配哪一个采样时间、哪一个质量标记。
 * 例如 sensor.value 的 observedAt 为 timestamp 时，读取 sensor.timestamp，而不是根记录的 timestamp。
 * 路径只经过已声明的对象字段，不从数组里挑一项或计算平均值；传输沿用现有 presentation 扩展。
 * 采样时间表示该值适用于对象的时刻，与计算完成、保存、查询时间分别声明，不能互相补齐。
 *
 * @param observedAt 相对当前结果所在记录的采样时间路径，读取结果中的 null 表示未声明
 * @param quality 相对当前结果所在记录的质量字段路径，读取结果中的 null 表示未声明
 * @since 1.3.3
 * @see org.jetlinks.core.annotation.Observation
 * @see SemanticRecord
 */
public record ObservationMapping(String observedAt, String quality) {
}
