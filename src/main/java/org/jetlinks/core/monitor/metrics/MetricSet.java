package org.jetlinks.core.monitor.metrics;

import org.hswebframework.web.exception.I18nSupportException;
import org.jetlinks.core.metadata.*;
import org.jetlinks.core.metadata.types.NumberType;
import org.jetlinks.core.metadata.types.ObjectType;
import org.jetlinks.core.metadata.unit.ValueUnits;
import org.jetlinks.core.utils.MetadataUtils;
import org.springframework.core.ResolvableType;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

/**
 * 按 public 契约共享的指标定义与读取器，不是对象注册表，也不保存目标实例。
 * 元数据统一来自 MetadataUtils；ClassValue 允许动态插件类随其类加载器卸载。
 * 中立定义按只读契约共享，消费边界按 Locale 读取文案，无需复制模型。
 *
 * @since 1.3.3
 * @see org.jetlinks.core.annotation.metric.Metric
 * @see Metrics#observe(String, Class, Object)
 */
public final class MetricSet {
    private static final ClassValue<MetricSet> definitions = new ClassValue<>() {
        @Override
        protected MetricSet computeValue(Class<?> type) {
            return new MetricSet(type);
        }
    };

    private final ObjectType definition = new ObjectType();
    private final List<Metric> metrics;

    private MetricSet(Class<?> contract) {
        if (!Modifier.isPublic(contract.getModifiers())) {
            throw invalid(contract.getName());
        }
        ExpandsMerge same = ExpandsMerge.requireEqual(Objects::deepEquals, () -> invalid("metric"));
        ExpandsMerge rules = ExpandsMerges.defaultStrategy().with("metric", ExpandsMerge.object(Map.of(
            "kind", same, "unit", same, "semantic", same)));
        List<Metric> parsed = new ArrayList<>();
        for (Map.Entry<Method, PropertyMetadata> entry : MetadataUtils
            .parseReadOnlyProperties(ResolvableType.forClass(contract), rules).entrySet()) {
            PropertyMetadata property = entry.getValue();
            Object value = property.getExpand("metric").orElse(null);
            if (value == null) {
                continue;
            }
            MetricKind kind = normalize(property, value);
            try {
                parsed.add(new Metric(property, entry.getKey(), kind));
            } catch (IllegalAccessException e) {
                throw invalid(entry.getKey().toGenericString());
            }
        }
        this.metrics = List.copyOf(parsed);
        definition.setProperties(metrics.stream().map(Metric::getMetadata).toList());
    }

    /**
     * 首次使用时解析契约；不检查或持有某个 target，也不执行 getter。
     *
     * @param contract public 观测接口或公开类，元数据不从 target 实现类推断
     * @return 按契约共享的定义
     */
    public static MetricSet of(Class<?> contract) {
        return definitions.get(Objects.requireNonNull(contract, "contract"));
    }

    public List<Metric> getMetrics() {
        return metrics;
    }

    /**
     * @return 共享的中立定义；调用方不得修改其属性、类型或扩展
     * @see PropertyMetadata#getI18nName(Locale)
     */
    public ObjectType getMetadata() {
        return definition;
    }

    private static MetricKind normalize(PropertyMetadata property, Object value) {
        if (!(value instanceof Map<?, ?> source)) {
            throw invalid(property.getId());
        }
        MetricKind kind = MetricKind.valueOf((String) source.get("kind"));
        MetricSemantic semantic = MetricSemantic.valueOf((String) source.get("semantic"));
        if ((kind == MetricKind.counter || semantic != MetricSemantic.value)
            && !(property.getValueType() instanceof NumberType<?>)) {
            throw invalid(property.getId());
        }
        // 注解参数的类型已由声明保证；这里只校验 DataType 是否支持所声明的语义。
        String unit = (String) source.remove("unit");
        if (unit != null) {
            if (!(property.getValueType() instanceof UnitSupported supported)) {
                throw invalid(property.getId() + ".unit");
            }
            supported.setUnit(ValueUnits.lookup(unit).orElseThrow(() -> invalid(property.getId() + ".unit")));
        }
        // 单位归一到标准 DataType，保留其余扩展，不重建另一份 metric 描述。
        return kind;
    }

    private static I18nSupportException invalid(String property) {
        return new I18nSupportException.NoStackTrace("error.invalid_metadata_semantics", property);
    }
}
