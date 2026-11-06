package org.jetlinks.core.monitor.observation;

import org.jetlinks.core.Key;

/**
 * 受限的元数据写入视图，不暴露 action 生命周期或权限上下文。
 *
 * 提取器只读取已持有的不可变引用/快照，不执行查询或业务副作用。
 * 每次观察最多 64 次来源写入，所有顶层集合累计最多 64 项。
 * observation.* 为框架保留字段；越界或提取失败不会提交当前回调的部分写入。
 *
 * @since 1.3.3
 * @see ObservationSpec
 */
public interface RecordMetadata {

    /**
     * 在当前提取回调中暂存标签，按 Key 的合并规则提交；不得跨回调保存此视图。
     *
     * @param key   非空的类型化键，不能使用 observation.* 保留字段
     * @param value 不可变引用或快照；null 表示忽略本次写入
     * @param <V>   键对应的值类型
     * @throws IllegalArgumentException 使用保留字段或超过预算；框架丢弃当前回调写入并隔离异常
     * @since 1.3.3
     */
    <V> void tag(Key<V> key, V value);

    /**
     * 在当前提取回调中暂存属性，提交与预算规则同标签；是否持久化由接收端决定。
     *
     * @param key   非空的类型化键，不能使用 observation.* 保留字段
     * @param value 不可变引用或快照；null 表示忽略本次写入
     * @param <V>   键对应的值类型
     * @throws IllegalArgumentException 使用保留字段或超过预算；框架丢弃当前回调写入并隔离异常
     * @since 1.3.3
     * @see #tag(Key, Object)
     */
    <V> void attribute(Key<V> key, V value);
}
