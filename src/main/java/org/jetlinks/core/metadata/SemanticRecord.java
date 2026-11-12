package org.jetlinks.core.metadata;

import org.jetlinks.core.metadata.types.ArrayType;
import org.jetlinks.core.metadata.types.DateTimeType;
import org.jetlinks.core.metadata.types.LongType;
import org.jetlinks.core.metadata.types.ObjectType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.math.BigDecimal;

/**
 * 按来源物模型读取一条已经拿到的数据，例如从设备温度快照中读值、采样时间和质量标记。
 * 由展示、投影等消费者调用，不发起查询、不检查权限、不修改或深拷贝原模型与记录。
 * 只访问最多八层的对象字段；多条记录不会自动取最新一条，多个结果也不会自动求和或选第一项。
 *
 * @since 1.3.3
 * @see MetadataSemantics
 */
public final class SemanticRecord {
    private static final int MAX_DEPTH = 8;

    private SemanticRecord() { }

    /**
     * 一个字段的读取结果，以及解析其相对时间、质量路径所需的同级记录。
     * 例如 path 为 sensor.value 时，owner 是 sensor 的模型，row 是 sensor 的数据，而不是根记录。
     *
     * @param field 来源字段模型
     * @param path 从本次读取根记录出发的完整路径
     * @param value 原字段值，允许为 null，0 和 false 保持不变
     * @param owner 直接包含该字段的对象模型
     * @param row 与 owner 对应的原记录，仅供读取
     */
    public record FieldValue(PropertyMetadata field, String path, Object value, ObjectType owner, Map<?, ?> row) { }

    /** @return 单条记录的模型；例如查询输出为“快照数组”时取快照模型，非对象输出返回 null */
    public static ObjectType objectType(DataType output) {
        if (output instanceof ArrayType array) output = array.getElementType();
        return output instanceof ObjectType type ? type : null;
    }

    /**
     * 按模型查字段，例如 sensor.value；不读取数据，也不支持 samples[0] 等数组下标或表达式。
     *
     * @return 来源已声明的字段；路径不存在、超过八层或需要经过数组时返回 null
     */
    public static PropertyMetadata field(DataType output, String path) {
        if (path == null) return null;
        String[] parts = path.split("\\.", -1);
        if (parts.length > MAX_DEPTH) return null;
        ObjectType owner = objectType(output);
        PropertyMetadata field = null;
        for (String part : parts) {
            if (owner == null || part.isEmpty()) return null;
            field = owner.getProperty(part).orElse(null);
            if (field == null) return null;
            owner = field.getValueType() instanceof ObjectType nested ? nested : null;
        }
        return field;
    }

    /**
     * 读取明确指定的字段，例如 sensor.value；不根据字段名称猜测含义或转换原始值。
     *
     * @return 字段及原值；模型路径或中间对象缺失时返回 null，末级值缺失时 FieldValue.value 为 null
     */
    public static FieldValue value(ObjectType type, Map<?, ?> row, String path) {
        PropertyMetadata field = field(type, path);
        if (field == null) return null;
        String[] parts = path.split("\\.", -1);
        for (int i = 0; i < parts.length - 1; i++) {
            if (!(row.get(parts[i]) instanceof Map<?, ?> nested)) return null;
            type = (ObjectType) type.getProperty(parts[i]).orElseThrow().getValueType();
            row = nested;
        }
        return new FieldValue(field, path, row.get(parts[parts.length - 1]), type, row);
    }

    /**
     * 按模型顺序枚举可读取字段，普通对象逐层展开，数组保持完整。
     * 已声明角色的值保持完整，例如 result 或 domain:position 是坐标对象时不拆成两条独立数值。
     *
     * @return 字段读取结果；自定义角色的原词和结构仍保存在原字段模型中
     */
    public static List<FieldValue> fields(ObjectType type, Map<?, ?> row) {
        return fields(type, row, "", 0);
    }

    private static List<FieldValue> fields(ObjectType type, Map<?, ?> row, String prefix, int depth) {
        if (depth >= MAX_DEPTH) return List.of();
        var result = new ArrayList<FieldValue>();
        for (var field : type.getProperties()) {
            String path = prefix.isEmpty() ? field.getId() : prefix + "." + field.getId();
            Object value = row.get(field.getId());
            if (field.getValueType() instanceof ObjectType nested && !nested.getProperties().isEmpty()
                && MetadataSemantics.propertyRoleTerm(field).isEmpty()
                && value instanceof Map<?, ?> map) result.addAll(fields(nested, map, path, depth + 1));
            else result.add(new FieldValue(field, path, value, type, row));
        }
        return result;
    }

    /**
     * 按内置角色读取当前层的唯一值，例如读取一条快照中唯一的 modelId。
     * 温度和压力都标为 result 时不能据此选择数值，应通过 value 明确读取字段路径。
     *
     * @return 原值；角色缺失、存在多个同角色字段或字段值为 null 时都返回 null
     */
    public static Object roleValue(ObjectType type, Map<?, ?> row, PropertySemanticRole role) {
        PropertyMetadata selected = roleField(type, role);
        return selected == null ? null : row.get(selected.getId());
    }

    private static PropertyMetadata roleField(ObjectType type, PropertySemanticRole role) {
        PropertyMetadata selected = null;
        for (var field : type.getProperties()) {
            if (MetadataSemantics.propertyRole(field).orElse(null) != role) continue;
            if (selected != null) return null;
            selected = field;
        }
        return selected;
    }

    /**
     * 读取唯一结果的同层说明，例如一条温度快照的 modelId 或 formattedValue。
     * 只有当前字段是本层唯一 result 时才关联；两个结果配一个单位/显示值不能确定对应关系。
     * 多结果应使用各自子记录、原多行输出或显式字段选择，不按位置或字段名配对。
     *
     * @return 唯一角色的原值；结果或说明不唯一时返回 null
     */
    public static Object resultQualifier(FieldValue item, PropertySemanticRole role) {
        return roleField(item.owner(), PropertySemanticRole.result) == item.field()
            ? roleValue(item.owner(), item.row(), role) : null;
    }

    /**
     * 读取唯一结果描述的属性 ID，例如 temperature。优先使用 observedProperty，
     * valueMetadata 可使用原 PropertyMetadata 或其 JSON 编码；二者矛盾时不猜测。
     * 属性定义本身由 resultQualifier 原样读取，不在此重建类型，避免丢失远端自定义类型和扩展。
     *
     * @return 结果描述的属性 ID；没有明确关联时返回 null，不修改来源模型或记录
     */
    public static String resultPropertyId(FieldValue item) {
        Object propertyId = resultQualifier(item, PropertySemanticRole.observedProperty);
        Object raw = resultQualifier(item, PropertySemanticRole.valueMetadata);
        Object metadataId = raw instanceof PropertyMetadata property ? property.getId()
            : raw instanceof Map<?, ?> map ? map.get("id") : null;
        if (propertyId instanceof String id) {
            return metadataId == null || id.equals(metadataId) ? id : null;
        }
        return metadataId instanceof String id ? id : null;
    }

    /**
     * 读取当前值声明的采样时间，例如从 value 的映射 timestamp 读取设备原始采样时间。
     * 现有 value 别名在没有映射时，仅兼容同层唯一结果、唯一 observedAt 字段的记录。
     *
     * @return Unix 毫秒时间；无法确定时返回 null，不用入库时间或本次查询时间补齐
     */
    public static Long observedAt(FieldValue item) {
        String path = MetadataSemantics.observation(item.field()).observedAt();
        // 现有快照曾仅以 value 和 observedAt 标记值与时间；两者都唯一时才有明确对应关系。
        if (path == null && "value".equals(item.field().getExpand(MetadataSemantics.PROPERTY_ROLE).orElse(null))
            && roleField(item.owner(), PropertySemanticRole.result) == item.field()) {
            PropertyMetadata time = roleField(item.owner(), PropertySemanticRole.observedAt);
            if (time != null) path = time.getId();
        }
        return path == null ? null : timestamp(value(item.owner(), item.row(), path));
    }

    /** @return 映射到的来源质量值，例如 valid=false 仍返回 false；未声明或缺少值时返回 null */
    public static Object quality(FieldValue item) {
        String path = MetadataSemantics.observation(item.field()).quality();
        FieldValue value = path == null ? null : value(item.owner(), item.row(), path);
        return value == null ? null : value.value();
    }

    /**
     * 将来源声明为 long/date 的时间字段读为 Unix 毫秒；来源应先约定时间单位，不在此猜测秒或毫秒。
     * 例如设备快照的 timestamp 已是毫秒，日期字段则使用原 DateTimeType 解析规则。
     *
     * @return 精确的有符号毫秒值，允许纪元前时间；缺少值、小数、溢出或无法解析的日期返回 null
     */
    public static Long timestamp(FieldValue item) {
        if (item == null || item.value() == null
            || !(item.field().getValueType() instanceof LongType || item.field().getValueType() instanceof DateTimeType)) return null;
        if (item.value() instanceof Number number) {
            try {
                return new BigDecimal(number.toString()).longValueExact();
            } catch (ArithmeticException | NumberFormatException invalid) {
                return null;
            }
        }
        if (item.field().getValueType() instanceof DateTimeType time) {
            try {
                return time.convert(item.value()).getTime();
            } catch (IllegalArgumentException | java.time.DateTimeException invalid) {
                return null;
            }
        }
        return null;
    }
}
