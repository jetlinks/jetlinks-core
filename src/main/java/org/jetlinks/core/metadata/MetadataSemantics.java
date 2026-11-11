package org.jetlinks.core.metadata;

import org.jetlinks.core.config.ConfigKey;
import org.hswebframework.web.exception.I18nSupportException;
import org.jetlinks.core.things.relation.Relation;
import org.jetlinks.core.things.relation.RelationSemantic;
import org.jetlinks.core.metadata.types.DateTimeType;
import org.jetlinks.core.metadata.types.LongType;
import org.jetlinks.core.metadata.types.ObjectType;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiPredicate;

/**
 * 统一声明和读取模型中的含义说明，例如“这是温度观测”“这个字段是采样时间”。
 * with 方法用于装配可修改模型，validateOutput 核对声明的主体和观测映射；
 * 读取方法返回内置词汇或完整词汇集合，不修改来源模型。
 * 自定义词仍保存在原扩展中；枚举视图为空只表示未识别，不能据此判断来源没有声明。
 * 这里不访问数据库、不执行命令，也不判断用户权限；数据及路径读取使用 SemanticRecord。
 *
 * @since 1.3.3
 * @see org.jetlinks.core.things.ThingMetadata
 */
public final class MetadataSemantics {

    public static final String SEMANTIC_TYPES_KEY = "semanticTypes";
    public static final String PROPERTY_ROLE_KEY = "semanticRole";
    public static final String FUNCTION_KIND_KEY = "functionKind";
    public static final String RELATION_SEMANTIC_KEY = "semantic";
    public static final String GRAPH_SUBJECT_KEY = "graphSubject";
    public static final String PRESENTATION_KEY = "presentation";
    public static final String PRIMARY_KEY = "primary";
    public static final String OBSERVED_AT_KEY = "observedAt";
    public static final String QUALITY_KEY = "quality";

    public static final ConfigKey<String[]> SEMANTIC_TYPES =
        ConfigKey.of(SEMANTIC_TYPES_KEY, SEMANTIC_TYPES_KEY, String[].class);

    public static final ConfigKey<String> PROPERTY_ROLE =
        ConfigKey.of(PROPERTY_ROLE_KEY, PROPERTY_ROLE_KEY, String.class);

    public static final ConfigKey<String> FUNCTION_KIND =
        ConfigKey.of(FUNCTION_KIND_KEY, FUNCTION_KIND_KEY, String.class);

    public static final ConfigKey<String> RELATION_SEMANTIC =
        ConfigKey.of(RELATION_SEMANTIC_KEY, RELATION_SEMANTIC_KEY, String.class);

    private static final Map<String, ExpandsMerge> mergeRules = Map.of(
        SEMANTIC_TYPES.getKey(), same(SEMANTIC_TYPES.getKey(), ExpandsMerge::unorderedEquals),
        PROPERTY_ROLE.getKey(), same(PROPERTY_ROLE.getKey(), Objects::deepEquals),
        FUNCTION_KIND.getKey(), same(FUNCTION_KIND.getKey(), Objects::deepEquals),
        GRAPH_SUBJECT_KEY, same(GRAPH_SUBJECT_KEY, Objects::deepEquals),
        PRESENTATION_KEY, ExpandsMerge.object(Map.of(
            PRIMARY_KEY, same(PRESENTATION_KEY + "." + PRIMARY_KEY, Objects::deepEquals),
            OBSERVED_AT_KEY, same(PRESENTATION_KEY + "." + OBSERVED_AT_KEY, Objects::deepEquals),
            QUALITY_KEY, same(PRESENTATION_KEY + "." + QUALITY_KEY, Objects::deepEquals)
        ))
    );

    /**
     * 为原元数据合并入口提供默认规则，例如字段和 getter 声明同一采样时间可以合并，
     * 声明不同采样时间则报冲突。通用入口通过 SPI 加载，调用者无需手动传入这套规则。
     *
     * @since 1.3.3
     * @see ExpandsMergeProvider
     */
    public static final class MergeProvider implements ExpandsMergeProvider {
        @Override
        public Map<String, ExpandsMerge> getRules() {
            return mergeRules;
        }
    }

    private static ExpandsMerge same(String key, BiPredicate<Object, Object> equality) {
        return ExpandsMerge.requireEqual(equality, () -> new I18nSupportException.NoStackTrace(
            "error.conflicting_metadata_semantics", key));
    }

    private MetadataSemantics() {
    }

    /**
     * 为可修改模型声明整体分类，例如动态指标输出同时属于 observation 和 result。
     * 返回原模型便于继续装配；与注解共用默认合并规则，矛盾声明报错，不覆盖已有含义。
     */
    public static <M extends Metadata> M withSemanticTypes(M metadata, SemanticType... types) {
        return declare(metadata, SEMANTIC_TYPES_KEY, Arrays.stream(types).map(Enum::name).toArray(String[]::new));
    }

    /** 为可修改字段声明内置用途，例如动态模型中的计数值标为 result；返回原字段。 */
    public static <P extends PropertyMetadata> P withPropertyRole(P property, PropertySemanticRole role) {
        return withPropertyRole(property, role.name());
    }

    /**
     * 为可修改字段声明开放词汇，例如 domain:position；消费者不认识时仍保留原词和完整值。
     * 调用方负责稳定词义，不能用显示名称代替角色。与内置枚举共用原扩展键和冲突规则。
     */
    public static <P extends PropertyMetadata> P withPropertyRole(P property, String role) {
        return declare(property, PROPERTY_ROLE_KEY, text(role, PROPERTY_ROLE_KEY));
    }

    /**
     * 为可修改字段声明相对采样时间和质量路径，例如 count 对应 sampledAt 和 valid。
     * 保留原 primary 等展示配置；冲突遵循默认合并规则，模型装配后使用 validateOutput 核对路径。
     */
    public static <P extends PropertyMetadata> P withObservation(P property, ObservationMapping mapping) {
        Map<String, Object> values = new LinkedHashMap<>();
        if (mapping.observedAt() != null && !mapping.observedAt().isEmpty()) values.put(OBSERVED_AT_KEY, mapping.observedAt());
        if (mapping.quality() != null && !mapping.quality().isEmpty()) values.put(QUALITY_KEY, mapping.quality());
        return declare(property, PRESENTATION_KEY, values);
    }

    private static <M extends Metadata> M declare(M metadata, String key, Object value) {
        Map<String, Object> expands = new LinkedHashMap<>();
        if (metadata.getExpands() != null) expands.putAll(metadata.getExpands());
        ExpandsMerges.defaultStrategy().merge(expands, Map.of(key, value));
        metadata.setExpands(expands);
        return metadata;
    }

    /**
     * 读取内置的整体分类，例如设备快照中的 observation 和 result。
     * 来源的自定义分类仍在原模型中；需要完整词汇时使用 semanticTypeTerms。
     *
     * @param metadata 来源模型
     * @return 不可变的已识别分类集合；未声明或只有自定义分类时为空
     */
    public static Set<SemanticType> semanticTypes(Metadata metadata) {
        EnumSet<SemanticType> result = EnumSet.noneOf(SemanticType.class);
        for (String term : semanticTypeTerms(metadata)) {
            parse(SemanticType.class, term).ifPresent(result::add);
        }
        return Collections.unmodifiableSet(result);
    }

    /** @return 去重后按来源顺序排列的完整分类词汇，包括 domain:reading 等自定义词；不修改原模型 */
    public static Set<String> semanticTypeTerms(Metadata metadata) {
        Object value = value(metadata.getExpands(), SEMANTIC_TYPES.getKey());
        if (value == null) {
            return Collections.emptySet();
        }
        Collection<?> terms;
        if (value instanceof String[]) {
            terms = Arrays.asList((String[]) value);
        } else if (value instanceof Collection<?>) {
            terms = (Collection<?>) value;
        } else {
            throw invalid(SEMANTIC_TYPES.getKey());
        }
        Set<String> result = new LinkedHashSet<>();
        for (Object term : terms) {
            result.add(text(term, SEMANTIC_TYPES.getKey()));
        }
        return Collections.unmodifiableSet(result);
    }

    /**
     * @return 已识别的字段角色，现有 value 别名统一为 result；
     * 未声明或自定义词返回空，原声明仍需从模型保留，非字符串等错误形状报错
     */
    public static Optional<PropertySemanticRole> propertyRole(PropertyMetadata property) {
        return propertyRoleTerm(property).flatMap(role -> parse(PropertySemanticRole.class, role))
            .map(role -> role == PropertySemanticRole.value ? PropertySemanticRole.result : role);
    }

    /** @return 原角色词，包括自定义词；空表示没有声明，不能用内置枚举识别结果代替此判断 */
    public static Optional<String> propertyRoleTerm(PropertyMetadata property) {
        return property.getExpand(PROPERTY_ROLE_KEY).map(role -> text(role, PROPERTY_ROLE_KEY));
    }

    /** @return 是否通过 GraphSubject 或 subjectId 声明“这条记录描述谁”；不证明该对象可访问 */
    public static boolean isSubject(PropertyMetadata property) {
        return Boolean.TRUE.equals(value(property.getExpands(), GRAPH_SUBJECT_KEY))
            || propertyRole(property).orElse(null) == PropertySemanticRole.subjectId;
    }

    /**
     * @return 字段声明的时间、质量路径，例如 value 对应 timestamp 和 valid；
     * 未声明的路径为 null，沿用原 presentation 编码，其他展示字段不参与解释
     */
    public static ObservationMapping observation(PropertyMetadata property) {
        Object declaration = value(property.getExpands(), PRESENTATION_KEY);
        if (declaration == null) return new ObservationMapping(null, null);
        if (!(declaration instanceof Map<?, ?> mapping)) throw invalid(property.getId() + "." + PRESENTATION_KEY);
        return new ObservationMapping(path(mapping, OBSERVED_AT_KEY), path(mapping, QUALITY_KEY));
    }

    private static String path(Map<?, ?> mapping, String key) {
        Object value = mapping.get(key);
        return value == null || "".equals(value) ? null : text(value, PRESENTATION_KEY + "." + key);
    }

    /** @return 命令声明的内置用途，例如读取快照为 query；未声明或自定义用途为空，不能默认允许执行 */
    public static Optional<FunctionKind> functionKind(FunctionMetadata function) {
        return term(function.getExpands(), FUNCTION_KIND.getKey(), FunctionKind.class);
    }

    /** @return 来源声明的内置关系分类，例如告警记录“描述设备”为 about；不根据数据推断关系或因果 */
    public static Optional<RelationSemantic> relationSemantic(Relation relation) {
        return term(relation.getExpands(), RELATION_SEMANTIC.getKey(), RelationSemantic.class);
    }

    /**
     * 校验传入对象这一层的声明，例如 value 指向的 timestamp 必须存在且声明为 long/date。
     * 同一层允许多个结果字段；两个主体字段、错误的映射路径会报错。自定义角色不因未识别而被拒绝。
     * 本方法不验证实例数据、跨字段业务关系或访问权限，也不递归校验子对象自己的映射。
     *
     * @param output 原输出对象模型，不修改元数据
     */
    public static void validateOutput(ObjectType output) {
        String subject = null;
        for (PropertyMetadata field : output.getProperties()) {
            propertyRole(field);
            Object marker = value(field.getExpands(), GRAPH_SUBJECT_KEY);
            if (marker != null && !(marker instanceof Boolean)) {
                throw invalid(field.getId() + ".graphSubject");
            }
            if (isSubject(field)) {
                if (subject != null) {
                    throw invalid(subject + ", " + field.getId() + ": graphSubject");
                }
                subject = field.getId();
            }
            ObservationMapping mapping = observation(field);
            if (mapping.quality() != null && SemanticRecord.field(output, mapping.quality()) == null) {
                throw invalid(field.getId() + ".presentation.quality: " + mapping.quality());
            }
            if (mapping.observedAt() == null) continue;
            PropertyMetadata target = SemanticRecord.field(output, mapping.observedAt());
            if (target == null || target.getValueType() == null
                || !(DateTimeType.ID.equals(target.getValueType().getId())
                    || LongType.ID.equals(target.getValueType().getId()))) {
                throw invalid(field.getId() + ".presentation.observedAt: " + mapping.observedAt());
            }
        }
    }

    private static Object value(Map<String, Object> expands, String key) {
        return expands == null ? null : expands.get(key);
    }

    private static <E extends Enum<E>> Optional<E> term(Map<String, Object> expands, String key, Class<E> type) {
        Object value = value(expands, key);
        return value == null ? Optional.empty() : parse(type, text(value, key));
    }

    private static String text(Object value, String key) {
        if (!(value instanceof String) || ((String) value).trim().isEmpty()) {
            throw invalid(key);
        }
        return (String) value;
    }

    private static I18nSupportException invalid(String key) {
        return new I18nSupportException.NoStackTrace("error.invalid_metadata_semantics", key);
    }

    private static <E extends Enum<E>> Optional<E> parse(Class<E> type, String term) {
        for (E constant : type.getEnumConstants()) {
            if (constant.name().equals(term)) {
                return Optional.of(constant);
            }
        }
        return Optional.empty();
    }
}
