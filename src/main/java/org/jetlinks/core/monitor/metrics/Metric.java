package org.jetlinks.core.monitor.metrics;

import org.jetlinks.core.metadata.PropertyMetadata;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;

/**
 * 一项共享的只读指标定义；仅绑定 public 契约方法，不捕获被观测实例或历史值。
 * 由 MetricSet 在契约首次解析时创建，可并发读取不同实例。
 *
 * @since 1.3.3
 * @see MetricSet
 */
public final class Metric {
    private final PropertyMetadata definition;
    private final MethodHandle reader;
    private final MetricKind kind;

    Metric(PropertyMetadata definition, Method method, MetricKind kind) throws IllegalAccessException {
        this.definition = definition;
        this.reader = MethodHandles.publicLookup().unreflect(method);
        this.kind = kind;
    }

    public String getId() {
        return definition.getId();
    }

    public MetricKind getKind() {
        return kind;
    }

    /**
     * 获取共享的中立定义；调用方只读，通过 getI18nName / getI18nDescription 获取文案。
     *
     * @return 标准属性元数据，不得修改其属性、类型或扩展
     */
    public PropertyMetadata getMetadata() {
        return definition;
    }

    /**
     * 读取给定实例的原始值，不清零、不缓存、不执行 I/O 保护或重试。
     * 契约 getter 应只读有界内存状态；读取方负责将单项异常表示为 unavailable。
     *
     * @param target 实现声明契约的运行对象，不得为 null
     * @return 保留 getter 的原始类型与 null 语义
     * @throws Throwable getter 原异常；类型不匹配亦直接失败
     */
    public Object read(Object target) throws Throwable {
        return reader.invoke(target);
    }
}
