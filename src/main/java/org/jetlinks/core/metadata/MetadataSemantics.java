package org.jetlinks.core.metadata;

import org.jetlinks.core.config.ConfigKey;
import org.hswebframework.web.exception.I18nSupportException;
import org.jetlinks.core.things.relation.Relation;
import org.jetlinks.core.things.relation.RelationSemantic;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 通用物模型的语义扩展键及只读视图，不解析业务路径或执行 I/O。
 * 未知词汇不参与内置枚举视图，原 expands 保持不变；错误形状直接拒绝。
 *
 * @since 1.3.3
 * @see org.jetlinks.core.things.ThingMetadata
 */
public final class MetadataSemantics {

    public static final ConfigKey<String[]> SEMANTIC_TYPES =
        ConfigKey.of("semanticTypes", "semanticTypes", String[].class);

    public static final ConfigKey<String> PROPERTY_ROLE =
        ConfigKey.of("semanticRole", "semanticRole", String.class);

    public static final ConfigKey<String> FUNCTION_KIND =
        ConfigKey.of("functionKind", "functionKind", String.class);

    public static final ConfigKey<String> RELATION_SEMANTIC =
        ConfigKey.of("semantic", "semantic", String.class);

    private MetadataSemantics() {
    }

    /**
     * 读取已识别的数据角色。缺省为空，数组/集合之外的声明为非法；不修改原模型。
     *
     * @param metadata 来源模型
     * @return 不可变的已识别角色
     */
    public static Set<SemanticType> semanticTypes(Metadata metadata) {
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
        EnumSet<SemanticType> result = EnumSet.noneOf(SemanticType.class);
        for (Object term : terms) {
            parse(SemanticType.class, text(term, SEMANTIC_TYPES.getKey())).ifPresent(result::add);
        }
        return Collections.unmodifiableSet(result);
    }

    /** @return 已识别的字段角色；缺省或未知词汇为空，错误形状失败 */
    public static Optional<PropertySemanticRole> propertyRole(PropertyMetadata property) {
        return term(property.getExpands(), PROPERTY_ROLE.getKey(), PropertySemanticRole.class);
    }

    /** @return 已识别的功能分类；未知不能被解释为只读或允许执行 */
    public static Optional<FunctionKind> functionKind(FunctionMetadata function) {
        return term(function.getExpands(), FUNCTION_KIND.getKey(), FunctionKind.class);
    }

    /** @return 规范方向上的已识别关系语义；不会推断来源事实 */
    public static Optional<RelationSemantic> relationSemantic(Relation relation) {
        return term(relation.getExpands(), RELATION_SEMANTIC.getKey(), RelationSemantic.class);
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
