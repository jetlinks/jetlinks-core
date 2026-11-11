package org.jetlinks.core.monitor.metrics;

import org.hswebframework.web.exception.I18nSupportException;
import org.jetlinks.core.metadata.*;
import org.jetlinks.core.metadata.types.NumberType;
import org.jetlinks.core.metadata.types.ObjectType;
import org.jetlinks.core.metadata.unit.ValueUnit;
import org.jetlinks.core.metadata.unit.ValueUnits;
import org.jetlinks.core.utils.MetadataUtils;
import org.springframework.core.ResolvableType;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

/**
 * 按 public 契约共享的指标定义与读取器，不是对象注册表，也不保存目标实例。
 * 元数据统一来自 MetadataUtils；ClassValue 允许动态插件类随其类加载器卸载。
 * 编译后的中立定义不对外暴露，消费边界按 Locale 生成独立视图。
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
            normalize(property, value);
            try {
                parsed.add(new Metric(property, entry.getKey()));
            } catch (IllegalAccessException e) {
                throw invalid(entry.getKey().toGenericString());
            }
            definition.addPropertyMetadata(property);
        }
        this.metrics = List.copyOf(parsed);
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

    public ObjectType getMetadata(Locale locale) {
        return MetadataUtils.localize(definition, locale);
    }

    private static void normalize(PropertyMetadata property, Object value) {
        if (!(value instanceof Map<?, ?> source)
            || !(source.get("kind") instanceof String kind)
            || !(source.get("semantic") instanceof String semantic)) {
            throw invalid(property.getId());
        }
        MetricKind parsedKind;
        MetricSemantic parsedSemantic;
        try {
            parsedKind = MetricKind.valueOf(kind);
            parsedSemantic = MetricSemantic.valueOf(semantic);
        } catch (IllegalArgumentException e) {
            throw invalid(property.getId());
        }
        if ((parsedKind == MetricKind.counter || parsedSemantic != MetricSemantic.value)
            && !(property.getValueType() instanceof NumberType<?>)) {
            throw invalid(property.getId());
        }
        if (parsedKind == MetricKind.counter && parsedSemantic != MetricSemantic.value) {
            throw invalid(property.getId());
        }
        Object unit = source.get("unit");
        if (unit != null) {
            if (!(unit instanceof String id) || !(property.getValueType() instanceof UnitSupported supported)) {
                throw invalid(property.getId() + ".unit");
            }
            ValueUnit resolved = ValueUnits.lookup(id).orElseThrow(() -> invalid(property.getId() + ".unit"));
            if (supported.getUnit() != null && !supported.getUnit().getId().equals(resolved.getId())) {
                throw invalid(property.getId() + ".unit");
            }
            supported.setUnit(resolved);
        }
        // 声明语法中的单位只用于构造标准 DataType；对外不维护两个权威单位。
        Map<String, Object> normalized = new LinkedHashMap<>();
        normalized.put("kind", parsedKind.name());
        normalized.put("semantic", parsedSemantic.name());
        property.getExpands().put("metric", normalized);
    }

    private static I18nSupportException invalid(String property) {
        return new I18nSupportException.NoStackTrace("error.invalid_metadata_semantics", property);
    }
}
