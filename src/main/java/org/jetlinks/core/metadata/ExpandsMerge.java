package org.jetlinks.core.metadata;

import org.springframework.util.ObjectUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.function.BinaryOperator;
import java.util.function.Supplier;

/**
 * 不可变的扩展合并规则。字段含义和冲突异常由定义方提供，不依赖具体词汇。
 * 仅显式声明的对象合并子键；等价比较保留原值，不规范化存储内容。
 *
 * @since 1.3.3
 * @see ExpandsMerges#defaultStrategy()
 */
public final class ExpandsMerge {

    public static final ExpandsMerge KEEP_FIRST = new ExpandsMerge((previous, addition) -> previous);

    public static final ExpandsMerge OVERWRITE = new ExpandsMerge((previous, addition) -> addition);

    private final Map<String, ExpandsMerge> fields;

    private final BinaryOperator<Object> merger;

    private ExpandsMerge(BinaryOperator<Object> merger) {
        this.fields = Map.of();
        this.merger = Objects.requireNonNull(merger, "merger");
    }

    private ExpandsMerge(Map<String, ExpandsMerge> fields) {
        this.fields = Map.copyOf(fields);
        this.merger = this::mergeObjects;
    }

    /**
     * 使用定义方的值合并函数。首次声明直接保留补充值；仅已有非 null 值时调用函数。
     *
     * @param merger 可并发复用的同步函数，不得修改输入；异常直接传播
     * @return 不可变规则，可用作对象子键、局部规则或注册默认规则
     * @since 1.3.3
     */
    public static ExpandsMerge of(BinaryOperator<Object> merger) {
        return new ExpandsMerge(merger);
    }

    /**
     * 显式合并对象子键；未声明的子键先声明优先，不递归合并其内容。
     * 非对象值仍先声明优先，形状校验由原模型定义负责。
     *
     * @param fields 子键规则
     * @return 可复用的对象合并规则
     */
    public static ExpandsMerge object(Map<String, ExpandsMerge> fields) {
        return new ExpandsMerge(fields);
    }

    /**
     * 已存在的值必须等价；冲突时使用定义方提供的异常。
     *
     * @param equality 等价比较，不得修改输入
     * @param conflict 冲突异常
     * @return 严格合并规则，等价时保留原值
     */
    public static ExpandsMerge requireEqual(BiPredicate<Object, Object> equality,
                                            Supplier<? extends RuntimeException> conflict) {
        return new ExpandsMerge((previous, addition) -> {
            if (!equality.test(previous, addition)) {
                throw conflict.get();
            }
            return previous;
        });
    }

    /**
     * 数组和集合按无序集合比较，忽略顺序及重复项；不筛选未知词或修改原值。
     * 非集合值沿用深度值比较，其有效性仍由原类型负责校验。
     *
     * @param previous 原值
     * @param addition 新值
     * @return 是否等价
     */
    public static boolean unorderedEquals(Object previous, Object addition) {
        Collection<?> left = collection(previous);
        Collection<?> right = collection(addition);
        return left != null && right != null
            ? new HashSet<>(left).equals(new HashSet<>(right))
            : Objects.deepEquals(previous, addition);
    }

    private static Collection<?> collection(Object value) {
        if (value instanceof Collection<?> collection) {
            return collection;
        }
        return value != null && value.getClass().isArray()
            ? Arrays.asList(ObjectUtils.toObjectArray(value))
            : null;
    }

    /**
     * 取得子键规则，未声明时使用调用方原有的覆盖策略。
     *
     * @param key 子键
     * @param fallback 原有策略
     * @return 合并规则
     */
    public ExpandsMerge field(String key, ExpandsMerge fallback) {
        return fields.getOrDefault(key, fallback);
    }

    /**
     * 按子键规则合并，未声明的键保留原值。只修改目标 map。
     *
     * @param target 目标扩展
     * @param additions 补充扩展
     */
    public void merge(Map<String, Object> target, Map<String, Object> additions) {
        additions.forEach((key, value) -> put(target, key, value, KEEP_FIRST));
    }

    /**
     * 合并单个子键；null 原值按尚未声明处理，沿用 putIfAbsent 约定。
     *
     * @param target 目标扩展
     * @param key 子键
     * @param value 补充值
     * @param fallback 未声明规则时的原有策略
     */
    public void put(Map<String, Object> target, String key, Object value, ExpandsMerge fallback) {
        target.put(key, field(key, fallback).apply(target.get(key), value));
    }

    private Object apply(Object previous, Object addition) {
        return previous == null ? addition : merger.apply(previous, addition);
    }

    private Object mergeObjects(Object previous, Object addition) {
        if (!(previous instanceof Map<?, ?> left) || !(addition instanceof Map<?, ?> right)) {
            return previous;
        }
        // 复制后再合并，避免修改共享注解或命令模型中的嵌套对象。
        Map<Object, Object> merged = new LinkedHashMap<>(left);
        right.forEach((key, value) -> {
            ExpandsMerge rule = fields.getOrDefault(key, KEEP_FIRST);
            merged.put(key, rule.apply(merged.get(key), value));
        });
        return merged;
    }
}
