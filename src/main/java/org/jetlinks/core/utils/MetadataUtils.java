package org.jetlinks.core.utils;

import com.fasterxml.jackson.annotation.JsonView;
import com.google.common.collect.Sets;
import io.swagger.v3.oas.annotations.ExternalDocumentation;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.annotations.extensions.Extensions;
import io.swagger.v3.oas.annotations.media.Schema;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.hswebframework.ezorm.core.CastUtil;
import org.hswebframework.web.dict.EnumDict;
import org.hswebframework.web.i18n.LocaleUtils;
import org.hswebframework.web.exception.I18nSupportException;
import org.jetlinks.core.annotation.Attr;
import org.jetlinks.core.annotation.Expands;
import org.jetlinks.core.annotation.GraphSubject;
import org.jetlinks.core.annotation.SemanticRole;
import org.jetlinks.core.annotation.ui.Presentation;
import org.jetlinks.core.metadata.*;
import org.jetlinks.core.metadata.types.*;
import org.jetlinks.core.things.ThingsConfigKeys;
import org.jetlinks.core.utils.json.ObjectMappers;
import org.reactivestreams.Publisher;
import org.springframework.core.ResolvableType;
import org.springframework.context.MessageSource;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.Order;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.annotation.RepeatableContainers;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.ObjectUtils;
import reactor.util.function.Tuples;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

/**
 * 物模型工具类
 *
 * @author zhouhao
 * @since 1.2.2
 */
public class MetadataUtils {

    /** 延迟国际化的资源键列表；值仍使用原有 name / description 作为默认文案。 */
    public static final String I18N_KEYS = "i18nKeys";

    /**
     * 解析物模型中的国际化消息,通过在物模型的{@link Metadata#getExpand(String)}中定义国际化信息.
     * <pre>{@code
     * {
     *     "id":"",
     *     "name:"",
     *     "expands":{
     *         "i18nMessages":{
     *             "name":{"zh":"名称","en":"name"},
     *             "description":{"zh":"描述","en":"description"}
     *         }
     *     }
     * }
     *
     * }</pre>
     *
     * @param locale     语言地区
     * @param metadata   物模型
     * @param key        消息key
     * @param defaultMsg 默认消息
     * @return 国际化消息
     * @see ThingsConfigKeys#i18nMessages
     */
    public static String resolveI18nMessage(Locale locale,
                                            Metadata metadata,
                                            String key,
                                            String defaultMsg) {
        return resolveI18nMessage(
            locale,
            metadata.getExpand(ThingsConfigKeys.i18nMessages).orElse(null),
            key,
            defaultMsg
        );
    }


    /**
     * 解析国际化消息,通过在物模型的{@link Metadata#getExpand(String)}中定义国际化信息.
     * <pre>{@code
     * {
     *    "name":{"zh":"名称","en":"name"},
     *    "description":{"zh":"描述","en":"description"}
     * }
     * }</pre>
     *
     * @param locale     语言地区
     * @param source     源数据
     * @param key        消息key
     * @param defaultMsg 默认消息
     * @return 国际化消息
     * @see ThingsConfigKeys#i18nMessages
     */
    public static String resolveI18nMessage(Locale locale,
                                            Map<String, Map<String, String>> source,
                                            String key,
                                            String defaultMsg) {
        if (MapUtils.isEmpty(source)) {
            return defaultMsg;
        }
        Map<String, String> i18n = source.get(key);
        if (MapUtils.isEmpty(i18n)) {
            return defaultMsg;
        }

        String msg = i18n.get(locale.toString());
        if (msg != null) {
            return msg;
        }

        msg = i18n.get(locale.getLanguage());
        if (msg != null) {
            return msg;
        }

        return defaultMsg;
    }


    /**
     * 根据类字段解析属性元数据
     *
     * @param field 字段
     * @param type  字段类型
     * @return 属性元数据
     */
    public static PropertyMetadata parseProperty(Field field, ResolvableType type) {
        return MetadataParser.withField(field, type);
    }

    /**
     * 解析无参方法属性，不执行方法；泛型以声明契约解析，不读取实现对象。
     *
     * @param owner 声明契约及其泛型
     * @param method 属性方法，可使用组合 Schema
     * @return 按当前语言生成的属性描述
     * @since 1.3.3
     */
    public static PropertyMetadata parseProperty(ResolvableType owner, Method method) {
        return parseProperty(owner, method, propertyName(method));
    }

    /**
     * 使用调用方已确定的属性身份解析方法；供保留原有命名规则的命令等入口复用。
     * 不负责发现方法或授权；不执行 getter。
     *
     * @param owner 声明契约
     * @param method 无参、非 void 方法
     * @param id 稳定属性 ID
     * @return 当前语言下的描述
     * @since 1.3.3
     */
    public static PropertyMetadata parseProperty(ResolvableType owner, Method method, String id) {
        return new MetadataParser().withMethod(owner, method, id);
    }

    /**
     * 解析可共享的中立方法属性，保留资源键与默认文案，不缓存当前用户语言。
     * 使用 {@link #localize(PropertyMetadata, Locale)} 在消费边界生成独立描述。
     *
     * @param owner 声明契约
     * @param method 无参、非 void 方法
     * @return 语言中立属性定义
     * @since 1.3.3
     */
    public static PropertyMetadata parsePropertyDefinition(ResolvableType owner, Method method) {
        return LocaleUtils.doWith(Locale.ROOT, () -> new MetadataParser(false, ExpandsMerges.defaultStrategy())
            .withMethod(owner, method, propertyName(method)));
    }

    /**
     * 显式发现带 Schema 或组合 Expands 声明的 public 只读 getter，返回语言中立定义。
     * hidden、static、桥接方法与 Object 方法不导出；现有 parseType 字段模式不受影响。
     *
     * @param type 声明契约，支持接口继承与泛型
     * @return 只读属性集合，不含目标实例或值
     * @since 1.3.3
     */
    public static ObjectType parseReadOnlyType(ResolvableType type) {
        return parseReadOnlyType(type, ExpandsMerges.defaultStrategy());
    }

    /**
     * 使用局部扩展合并规则发现只读属性；规则不修改全局策略。
     * 同名不同类型属性或局部规则拒绝的冲突直接失败，不按反射顺序覆盖。
     *
     * @param type 声明契约
     * @param rules 本次解析的不可变合并规则
     * @return 语言中立属性集合
     * @since 1.3.3
     */
    public static ObjectType parseReadOnlyType(ResolvableType type, ExpandsMerge rules) {
        ObjectType object = new ObjectType();
        parseReadOnlyProperties(type, rules).values().forEach(object::addPropertyMetadata);
        object.setExpands(parseExpands(type.toClass(), rules));
        return object;
    }

    /**
     * 发现只读属性并保留其公开读取方法；方法仅供进程内绑定，不进入元数据协议。
     * 属性解析、命名、选择与排序和 parseReadOnlyType 使用同一实现，不执行 getter。
     *
     * @param type 声明契约
     * @param rules 局部扩展合并规则
     * @return 按属性顺序排列的方法与中立描述
     * @since 1.3.3
     */
    public static Map<Method, PropertyMetadata> parseReadOnlyProperties(ResolvableType type, ExpandsMerge rules) {
        return LocaleUtils.doWith(Locale.ROOT, () -> {
            MetadataParser parser = new MetadataParser(false, rules);
            Map<String, PropertyMetadata> properties = new LinkedHashMap<>();
            Map<String, Method> readers = new LinkedHashMap<>();
            Method[] methods = type.toClass().getMethods();
            Arrays.sort(methods, Comparator.comparing(Method::toGenericString));
            for (Method method : methods) {
                if (!isReadOnlyProperty(method)) {
                    continue;
                }
                Schema schema = AnnotatedElementUtils.findMergedAnnotation(method, Schema.class);
                if (schema != null && schema.hidden()) {
                    continue;
                }
                if (schema == null && Arrays.stream(propertyAnnotations(method))
                    .allMatch(annotation -> expandAnnotations(annotation).isEmpty())) {
                    continue;
                }
                PropertyMetadata property = parser.withMethod(type, method, propertyName(method));
                PropertyMetadata previous = properties.putIfAbsent(property.getId(), property);
                readers.putIfAbsent(property.getId(), method);
                if (previous != null) {
                    if (!readers.get(property.getId()).getName().equals(method.getName())
                        || !Objects.equals(previous.getValueType().toJson(), property.getValueType().toJson())) {
                        throw new I18nSupportException.NoStackTrace("error.conflicting_metadata_semantics", property.getId());
                    }
                    mergeExpands(previous.getExpands(), property.getExpands(), rules);
                }
            }
            Map<Method, PropertyMetadata> result = new LinkedHashMap<>();
            properties.values().stream()
                .sorted(Comparator.comparingInt((PropertyMetadata p) -> p.getExpand("order")
                    .filter(Number.class::isInstance).map(Number.class::cast).map(Number::intValue)
                    .orElse(Integer.MAX_VALUE)).thenComparing(PropertyMetadata::getId))
                .forEach(property -> result.put(readers.get(property.getId()), property));
            return Collections.unmodifiableMap(result);
        });
    }

    private static boolean isReadOnlyProperty(Method method) {
        String name = method.getName();
        return Modifier.isPublic(method.getModifiers()) && !Modifier.isStatic(method.getModifiers())
            && !method.isBridge() && !method.isSynthetic() && method.getDeclaringClass() != Object.class
            && method.getParameterCount() == 0 && method.getReturnType() != void.class
            && method.getReturnType() != Void.class
            && ((name.startsWith("get") && name.length() > 3)
            || (name.startsWith("is") && name.length() > 2
            && (method.getReturnType() == boolean.class || method.getReturnType() == Boolean.class)));
    }

    private static String propertyName(Method method) {
        Schema schema = AnnotatedElementUtils.findMergedAnnotation(method, Schema.class);
        if (schema != null && StringUtils.hasText(schema.name())) {
            return schema.name();
        }
        String name = method.getName();
        int prefix = name.startsWith("get") && name.length() > 3 ? 3
            : name.startsWith("is") && name.length() > 2 ? 2 : 0;
        return Introspector.decapitalize(name.substring(prefix));
    }

    /**
     * 生成指定语言的独立属性视图，不修改共享定义；内嵌 i18nMessages 优先于资源键。
     * 仅用于元数据发现边界，不在每次指标采样时调用。
     *
     * @param definition 中立属性定义
     * @param locale 请求语言
     * @return 独立的可序列化描述
     * @since 1.3.3
     */
    public static PropertyMetadata localize(PropertyMetadata definition, Locale locale) {
        return localizePropertyView(definition, locale, (key, fallback) -> LocaleUtils.resolveMessage(key, locale, fallback));
    }

    /**
     * 使用显式消息源生成独立属性视图；适用于隔离的应用上下文，不替换进程默认消息源。
     *
     * @param definition 中立定义
     * @param locale 请求语言
     * @param source 已配置的消息源
     * @return 独立属性描述
     * @since 1.3.3
     */
    public static PropertyMetadata localize(PropertyMetadata definition, Locale locale, MessageSource source) {
        Objects.requireNonNull(source, "source");
        return localizePropertyView(definition, locale,
            (key, fallback) -> LocaleUtils.resolveMessage(source, locale, key, fallback));
    }

    private static PropertyMetadata localizePropertyView(PropertyMetadata definition, Locale locale,
                                                         BiFunction<String, String, String> messages) {
        Objects.requireNonNull(locale, "locale");
        SimplePropertyMetadata copy = new SimplePropertyMetadata();
        copy.fromJson(com.alibaba.fastjson.JSON.parseObject(definition.toJson().toJSONString()));
        localizeProperty(copy, locale, messages);
        return copy;
    }

    /**
     * 为共享对象属性定义生成指定语言的独立视图，不保留请求 Locale。
     *
     * @param definition 语言中立类型
     * @param locale 请求语言
     * @return 包含嵌套属性与枚举文案的独立类型
     * @since 1.3.3
     */
    public static ObjectType localize(ObjectType definition, Locale locale) {
        return localizeObjectView(definition, locale, (key, fallback) -> LocaleUtils.resolveMessage(key, locale, fallback));
    }

    /**
     * 使用显式消息源本地化对象类型，避免隔离应用或测试修改全局消息源。
     *
     * @param definition 中立类型
     * @param locale 请求语言
     * @param source 已配置的消息源
     * @return 独立类型描述
     * @since 1.3.3
     */
    public static ObjectType localize(ObjectType definition, Locale locale, MessageSource source) {
        Objects.requireNonNull(source, "source");
        return localizeObjectView(definition, locale,
            (key, fallback) -> LocaleUtils.resolveMessage(source, locale, key, fallback));
    }

    private static ObjectType localizeObjectView(ObjectType definition, Locale locale,
                                                  BiFunction<String, String, String> messages) {
        Objects.requireNonNull(locale, "locale");
        ObjectType copy = new ObjectType();
        copy.fromJson(com.alibaba.fastjson.JSON.parseObject(definition.toJson().toJSONString()));
        localizeType(copy, locale, messages);
        return copy;
    }

    private static void localizeProperty(PropertyMetadata property, Locale locale, BiFunction<String, String, String> messages) {
        SimplePropertyMetadata mutable = (SimplePropertyMetadata) property;
        mutable.setName(localizedText(property, locale, "name", property.getName(), messages));
        mutable.setDescription(localizedText(property, locale, "description", property.getDescription(), messages));
        localizeType(property.getValueType(), locale, messages);
    }

    private static String localizedText(Metadata metadata, Locale locale, String attribute, String fallback,
                                        BiFunction<String, String, String> messages) {
        Object keys = metadata.getExpand(I18N_KEYS).orElse(null);
        if (keys instanceof Map<?, ?> map && map.get(attribute) instanceof List<?> candidates) {
            for (int i = candidates.size() - 1; i >= 0; i--) {
                if (candidates.get(i) instanceof String key) {
                    fallback = messages.apply(key, fallback);
                }
            }
        }
        return resolveI18nMessage(locale, metadata, attribute, fallback);
    }

    private static void localizeType(DataType type, Locale locale, BiFunction<String, String, String> messages) {
        if (type instanceof ObjectType object) {
            object.getProperties().forEach(property -> localizeProperty(property, locale, messages));
        } else if (type instanceof ArrayType array && array.getElementType() != null) {
            localizeType(array.getElementType(), locale, messages);
        } else if (type instanceof EnumType enumeration && enumeration.getElements() != null) {
            enumeration.getElements().forEach(element -> element.setText(
                localizedText(enumeration, locale, element.getValue(), element.getText(), messages)));
        }
    }

    /**
     * 根据java类型解析物模型类型
     *
     * @param type java类型
     * @return 物模型类型
     */
    public static DataType parseType(ResolvableType type) {
        return MetadataParser.withType(type);
    }

    public static DataType parseType(Object value) {
        if (value instanceof Class<?> clazz) {
            return MetadataParser.withType(ResolvableType.forClass(clazz));
        }
        if (value instanceof Map<?, ?> _map) {
            return parseType(_map);
        }
        return MetadataParser.withType(ResolvableType.forClass(value.getClass()));
    }

    public static DataType parseType(Map<?, ?> map) {
        ObjectType dataType = new ObjectType();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            String key = String.valueOf(entry.getKey());
            Object value = entry.getValue();
            SimplePropertyMetadata metadata = new SimplePropertyMetadata();
            metadata.setId(key);
            metadata.setName(key);
            if (entry.getValue() != null) {
                metadata.setValueType(parseType(value));
            } else {
                metadata.setValueType(new UnknownType());
            }
            dataType.addPropertyMetadata(metadata);
        }
        return dataType;
    }


    /**
     * 解析拓展信息
     *
     * @param annotations 注解对象
     * @return 属性元数据
     */
    public static Map<String, Object> parseExpands(Annotation... annotations) {
        return parseExpands(ExpandsMerges.defaultStrategy(), annotations);
    }

    /**
     * 解析拓展信息
     *
     * @param element 元素
     * @return 属性元数据
     */
    public static Map<String, Object> parseExpands(AnnotatedElement element) {
        return parseExpands(element, ExpandsMerges.defaultStrategy());
    }

    /**
     * 按显式局部规则解析扩展，不读取或修改全局默认策略。
     *
     * @param rules 合并规则
     * @param annotations 注解对象
     * @return 扩展信息
     */
    public static Map<String, Object> parseExpands(ExpandsMerge rules, Annotation... annotations) {
        Map<String, Object> expands = new HashMap<>();
        MetadataParser.parseExpands(annotations, true, expands, rules);
        return expands;
    }

    /**
     * 按定义方提供的规则解析元素上的扩展。
     *
     * @param element 来源元素
     * @param rules 合并规则
     * @return 扩展信息
     */
    public static Map<String, Object> parseExpands(AnnotatedElement element, ExpandsMerge rules) {
        if (!(element instanceof Method method)) {
            return parseExpands(rules, element.getAnnotations());
        }
        Map<String, Object> expands = new HashMap<>();
        // 按声明层级分别解析：普通命名空间保留最近声明，严格语义仍能发现父子冲突。
        for (List<Annotation> annotations : methodAnnotations(method).values()) {
            mergeExpands(expands, parseExpands(rules, annotations.toArray(new Annotation[0])), rules);
        }
        return expands;
    }

    private static Annotation[] propertyAnnotations(AnnotatedElement element) {
        if (!(element instanceof Method)) {
            return element.getAnnotations();
        }
        return methodAnnotations((Method) element).values().stream().flatMap(Collection::stream)
            .toArray(Annotation[]::new);
    }

    private static Map<Integer, List<Annotation>> methodAnnotations(Method method) {
        // 方法注解不受 Java @Inherited 支持；保留来源层级，由局部规则决定覆盖或拒绝。
        Map<Integer, List<Annotation>> annotations = new TreeMap<>();
        // Expands.value 是 Attr[]，不能让标准 repeatable 容器识别把其命名空间解包丢失。
        MergedAnnotations.from(method, MergedAnnotations.SearchStrategy.TYPE_HIERARCHY, RepeatableContainers.none()).stream()
            .filter(annotation -> annotation.getDistance() == 0)
            .forEach(annotation -> annotations.computeIfAbsent(annotation.getAggregateIndex(), ignored -> new ArrayList<>())
                .add(annotation.synthesize()));
        return annotations;
    }

    private static Set<Expands> expandAnnotations(Annotation annotation) {
        Set<Expands> expands = new LinkedHashSet<>();
        if (annotation instanceof Expands value) {
            expands.add(value);
        } else if (annotation instanceof Expands.List values) {
            expands.addAll(Arrays.asList(values.value()));
        } else {
            expands.addAll(AnnotatedElementUtils.findMergedRepeatableAnnotations(annotation.annotationType(), Expands.class));
            Expands value = AnnotatedElementUtils.findMergedAnnotation(annotation.annotationType(), Expands.class);
            if (value != null) {
                expands.add(value);
            }
        }
        return expands;
    }

    /** 合并原元数据扩展；定义层声明冲突及对象规则，其余扩展保持原先声明优先。 */
    public static void mergeExpands(Map<String, Object> target, Map<String, Object> additions) {
        mergeExpands(target, additions, ExpandsMerges.defaultStrategy());
    }

    /**
     * 按显式规则合并扩展；不修改补充来源或建立新的传输字段。
     *
     * @param target 目标扩展
     * @param additions 补充扩展
     * @param rules 定义方提供的合并规则
     */
    public static void mergeExpands(Map<String, Object> target,
                                    Map<String, Object> additions,
                                    ExpandsMerge rules) {
        rules.merge(target, additions);
    }


    public static void resolveAttrs(Attr[] attrs, BiConsumer<String, Object> handler) {
        resolveAttrs(Arrays.asList(attrs), handler);
    }

    public static void resolveAttrs(Iterable<Attr> attrs, BiConsumer<String, Object> handler) {
        for (Attr attr : attrs) {
            Object value = parseAttrValue(attr);
            if (!attr.ignoreEmpty() || !ObjectUtils.isEmpty(value)) {
                handler.accept(attr.key(), value);
            }
        }
    }

    public static Object parseAttrValue(Attr attr) {
        if (StringUtils.hasText(attr.type())) {
            DataType type = DataTypes.lookup(attr.type()).get();
            if (type instanceof Converter) {
                return ((Converter<?>) type).convert(attr.value());
            }
            return type.validate(attr.value()).assertSuccess();
        }
        return attr.value();
    }

    static class MetadataParser {

        static final Set<String> jsr303Packages = Sets.newHashSet(
            "javax.validation.constraints",
            "jakarta.validation.constraints",
            "org.hibernate.validator.constraints"
        );

        static final Set<String> requiredValidators = Sets.newHashSet(
            "NotNull",
            "NotBlank",
            "NotEmpty"
        );

        Set<Object> distinct = new HashSet<>();

        private final ExpandsMerge expandsMerge;

        private final boolean resolveMessages;

        MetadataParser() {
            this(true, ExpandsMerges.defaultStrategy());
        }

        MetadataParser(boolean resolveMessages, ExpandsMerge expandsMerge) {
            this.resolveMessages = resolveMessages;
            this.expandsMerge = expandsMerge;
        }

        public static PropertyMetadata withField(Field field, ResolvableType type) {
            return new MetadataParser().withField0(field.getDeclaringClass(), field, type);
        }

        public static DataType withType(ResolvableType type) {
            return new MetadataParser().withType0(null, type);
        }

        static String resolveJsonViewName(Class<?> clazz) {
            String name = clazz.getCanonicalName();
            return name.contains(".") ? name.substring(name.lastIndexOf(".") + 1) : name;
        }

        static void parseJsonView(Annotation[] annotation,
                                  Map<String, Object> container) {
            for (Annotation ann : annotation) {
                if (ann instanceof JsonView view) {
                    container.put(
                        "jsonView",
                        Arrays.stream(view.value())
                              .map(MetadataParser::resolveJsonViewName)
                              .collect(Collectors.toSet()));
                }
            }
        }

        static void parseJsr303(Annotation[] annotation,
                                Map<String, Object> container) {
            List<Map<String, Object>> validators = new ArrayList<>();
            for (Annotation ann : annotation) {
                if (jsr303Packages.contains(ann.annotationType().getPackage().getName())) {
                    Map<String, Object> validator =
                        new HashMap<>(AnnotationUtils.getAnnotationAttributes(
                            ann,
                            true,
                            true));
                    String type = ann.annotationType().getSimpleName();
                    validator.put("type", type);
                    if (requiredValidators.contains(type)) {
                        container.putIfAbsent("required", true);
                    }
                    validator.compute("groups", (ignore, groups) -> {
                        if (groups instanceof String[]) {
                            @SuppressWarnings("all")
                            String[] lst = ((String[]) groups);
                            if (lst.length == 0) {
                                return null;
                            }
                            for (int i = 0; i < lst.length; i++) {
                                if (lst[i].contains(".")) {
                                    lst[i] = lst[i].substring(lst[i].lastIndexOf('.') + 1);
                                }
                            }
                            return lst;
                        }
                        return null;
                    });
                    validator.remove("payload");
                    validator.remove("message");
                    validators.add(validator);
                }
            }
            if (!validators.isEmpty()) {
                container.putIfAbsent("validators", validators);
            }
        }

        static void parseSwagger(Annotation[] annotation,
                                 Map<String, Object> container) {
            Schema schema = null;
            ExternalDocumentation doc = null;
            List<Extension> extensions = new ArrayList<>();
            for (Annotation ann : annotation) {
                if (ann instanceof Schema _schema) {
                    schema = _schema;
                    extensions.addAll(Arrays.asList(_schema.extensions()));
                }
                if (ann instanceof ExternalDocumentation _doc) {
                    doc = _doc;
                    extensions.addAll(Arrays.asList(_doc.extensions()));
                }
                if (ann instanceof Extension _extension) {
                    extensions.add(_extension);
                }
                if (ann instanceof Extensions _extensions) {
                    extensions.addAll(Arrays.asList(_extensions.value()));
                }
            }

            // 文档
            {
                ExternalDocumentation _doc = schema != null && StringUtils.hasText(schema.externalDocs().url())
                    ? schema.externalDocs()
                    : doc;

                if (_doc != null && StringUtils.hasText(_doc.url())) {
                    Object docRef = Map.of(
                        "description", _doc.description(),
                        "url", _doc.url()
                    );
                    container
                        .compute("externalDocs", (_key, val) -> {
                            if (val == null) {
                                return List.of(docRef);
                            }
                            if (val instanceof Collection<?> v) {
                                List<Object> docs = new ArrayList<>(v);
                                docs.add(docRef);
                                return docs;
                            }
                            return List.of(val, docRef);
                        });
                }
            }
            for (Extension extension : extensions) {
                mergeSwaggerExtension(container, extension);
            }
        }

        private static void mergeSwaggerExtension(Map<String, Object> container, Extension extension) {
            if (extension == null || !StringUtils.hasText(extension.name())) {
                return;
            }
            Map<String, Object> value = new LinkedHashMap<>();
            for (ExtensionProperty property : extension.properties()) {
                if (property != null && StringUtils.hasText(property.name())) {
                    value.put(property.name(), swaggerExtensionPropertyValue(property));
                }
            }
            container.put(extension.name(), value);
        }

        private static Object swaggerExtensionPropertyValue(ExtensionProperty property) {
            String value = property.value();
            if (!property.parseValue()) {
                return value;
            }
            try {
                return ObjectMappers.JSON_MAPPER.readValue(value, Object.class);
            } catch (Exception ignore) {
                return value;
            }
        }

        static void parseAttr(AnnotatedElement element,
                              Map<String, Object> container,
                              ExpandsMerge rules) {

            Set<Attr> attrs = AnnotatedElementUtils
                .findMergedRepeatableAnnotations(
                    element,
                    Attr.class
                );

            resolveAttrs(attrs, (key, value) -> rules.put(container, key, value, ExpandsMerge.KEEP_FIRST));


        }

        static void parseExpands(Annotation[] annotation,
                                 boolean includeName,
                                 Map<String, Object> container,
                                 ExpandsMerge rules) {
            for (Annotation ann : annotation) {
                parseExpands(new Annotation[]{ann}, includeName, container, rules, MergedAnnotations.from(ann));
            }
            parseJsr303(annotation, container);
            parseJsonView(annotation, container);
            parseSwagger(annotation, container);
        }

        private static void parseExpands(Annotation[] annotation,
                                         boolean includeName,
                                         Map<String, Object> container,
                                         ExpandsMerge rules,
                                         MergedAnnotations context) {
            for (Annotation original : annotation) {
                Annotation ann = original;
                if (!(ann instanceof Expands) && !(ann instanceof Expands.List)) {
                    MergedAnnotation<?> merged = context.get(ann.annotationType());
                    if (merged.isPresent()) {
                        ann = merged.synthesize();
                    }
                }

                Set<Expands> expandsSet = expandAnnotations(ann);


                if (CollectionUtils.isNotEmpty(expandsSet)) {
                    for (Expands exp : expandsSet) {
                        Map<String, Object> c = container;
                        ExpandsMerge childRules = rules;

                        boolean namespaced = includeName && StringUtils.hasText(exp.key());
                        if (namespaced) {
                            c = new HashMap<>();
                            childRules = rules.field(exp.key(), ExpandsMerge.OVERWRITE);
                        }
                        // 注解继承的方式
                        if (ann.annotationType() != Expands.class &&
                            ann.annotationType() != Expands.List.class) {
                            AnnotationAttributes annotationAttributes = AnnotationUtils
                                .getAnnotationAttributes(ann, false, false);
                            for (Map.Entry<String, Object> entry : annotationAttributes.entrySet()) {
                                Method attribute = ReflectionUtils.findMethod(ann.annotationType(), entry.getKey());
                                Attr mapping = attribute == null ? null : attribute.getAnnotation(Attr.class);
                                if (mapping != null) {
                                    Object value = annotationValue(entry.getValue());
                                    if (!mapping.ignoreEmpty() || !ObjectUtils.isEmpty(value)) {
                                        childRules.put(c, mapping.key(), value, ExpandsMerge.KEEP_FIRST);
                                    }
                                    continue;
                                }
                                if (entry.getValue() instanceof Class<?>) {
                                    DataType parseType = parseType(ResolvableType.forClass(CastUtil.cast(entry.getValue())));
                                    if (parseType instanceof ObjectType objectType) {
                                        List<PropertyMetadata> properties = objectType.getProperties();
                                        childRules.put(c, entry.getKey(), properties, ExpandsMerge.KEEP_FIRST);
                                    } else if (parseType != null) {
                                        childRules.put(c, entry.getKey(), parseType, ExpandsMerge.KEEP_FIRST);
                                    }
                                } else if (entry.getValue() instanceof Annotation _ann) {
                                    if (_ann instanceof org.jetlinks.core.annotation.DataType _dataType) {
                                        Class<?> clazz = _dataType.value();
                                        Class<?>[] gen = _dataType.generics();
                                        ResolvableType type = gen.length == 0 ? ResolvableType.forType(clazz) :
                                            ResolvableType.forClassWithGenerics(clazz, gen);
                                        childRules.put(c, entry.getKey(), parseType(type), ExpandsMerge.OVERWRITE);
                                    } else {
                                        childRules.put(c, entry.getKey(),
                                                       MetadataUtils.parseExpands(
                                                           childRules.field(entry.getKey(), ExpandsMerge.KEEP_FIRST), _ann),
                                                       ExpandsMerge.OVERWRITE);
                                    }
                                } else {
                                    childRules.put(c, entry.getKey(), entry.getValue(), ExpandsMerge.KEEP_FIRST);
                                }
                            }
                            Annotation[] meta = ann.annotationType().getAnnotations();
                            parseExpands(meta, false, c, childRules, context);
                            parseJsr303(meta, c);
                            parseJsonView(meta, c);
                            parseSwagger(meta, c);
                            parseAttr(ann.annotationType(), c, childRules);
                        }
                        // 直接定义了attr
                        Map<String, Object> target = c;
                        ExpandsMerge valueRules = childRules;
                        resolveAttrs(exp.value(), (key, value) -> valueRules.put(target, key, value, ExpandsMerge.KEEP_FIRST));
                        if (namespaced) {
                            // 普通命名空间保留整体覆盖；只有定义方显式声明时才合并子键。
                            rules.put(container, exp.key(), c, ExpandsMerge.OVERWRITE);
                        }
                    }
                }
            }

        }

        /** Only explicitly mapped annotation members use the stable scalar metadata vocabulary. */
        private static Object annotationValue(Object value) {
            if (value instanceof Enum<?> term) {
                return term.name();
            }
            if (value instanceof Enum<?>[] terms) {
                return Arrays.stream(terms).map(Enum::name).toArray(String[]::new);
            }
            return value;
        }

        private void parseExpands(AnnotatedElement element,
                                 boolean includeName,
                                 Map<String, Object> container) {
            if (element instanceof Method) {
                MetadataUtils.parseExpands(element, expandsMerge)
                    .forEach((key, value) -> expandsMerge.put(container, key, value, ExpandsMerge.KEEP_FIRST));
            } else {
                parseExpands(element.getAnnotations(), includeName, container, expandsMerge);
            }
        }

        private PropertyMetadata withMethod(ResolvableType owner, Method method, String id) {
            if (method.getParameterCount() != 0 || method.getReturnType() == void.class
                || method.getReturnType() == Void.class || !StringUtils.hasText(id)) {
                throw new I18nSupportException.NoStackTrace("error.invalid_metadata_semantics", method.toGenericString());
            }
            Schema schema = AnnotatedElementUtils.findMergedAnnotation(method, Schema.class);
            SimplePropertyMetadata metadata = new SimplePropertyMetadata();
            metadata.setId(id);
            metadata.setName(id);
            metadata.setValueType(withType0(method, ResolvableType.forType(
                method.getGenericReturnType(), owner.as(method.getDeclaringClass()))));
            metadata.setExpands(MetadataUtils.parseExpands(method, expandsMerge));
            if (schema != null) {
                List<String> sources = new ArrayList<>();
                if (!resolveMessages) {
                    sources.add(owner.toClass().getName() + "." + id);
                }
                sources.add(method.getDeclaringClass().getName() + "." + id);
                MergedAnnotation<?> annotation = MergedAnnotations
                    .from(method, MergedAnnotations.SearchStrategy.TYPE_HIERARCHY).get(Schema.class).getMetaSource();
                while (annotation != null) {
                    sources.add(annotation.getType().getName());
                    annotation = annotation.getMetaSource();
                }
                describe(metadata, schema, sources.stream().distinct().toList(), true);
                metadata.expand("hidden", schema.hidden());
            } else if (!resolveMessages) {
                retainMessageKeys(metadata, List.of(owner.toClass().getName() + "." + id,
                    method.getDeclaringClass().getName() + "." + id).stream().distinct().toList());
            }
            Order order = AnnotatedElementUtils.findMergedAnnotation(method, Order.class);
            if (order != null) {
                metadata.expand("order", order.value());
            }
            return metadata;
        }

        private void describe(SimplePropertyMetadata metadata, Schema schema, List<String> keys, boolean method) {
            String description = schema.description();
            String title = schema.title();
            if (method || StringUtils.hasText(description)) {
                metadata.setDescription(resolveMessages ? resolve(keys, "", description) : description);
                metadata.setName(metadata.getDescription());
            }
            if (StringUtils.hasText(title)) {
                metadata.setName(resolveMessages ? resolve(keys, ".title", title) : title);
            }
            if (!resolveMessages) {
                if (!StringUtils.hasText(metadata.getName())) {
                    metadata.setName(metadata.getId());
                }
                retainMessageKeys(metadata, keys);
            }
        }

        private void retainMessageKeys(SimplePropertyMetadata metadata, List<String> keys) {
            metadata.expand(I18N_KEYS, Map.of(
                "name", keys.stream().flatMap(key -> java.util.stream.Stream.of(key + ".title", key)).toList(),
                "description", keys));
        }

        private String resolve(List<String> keys, String suffix, String fallback) {
            for (int i = keys.size() - 1; i >= 0; i--) {
                fallback = LocaleUtils.resolveMessage(keys.get(i) + suffix, fallback);
            }
            return fallback;
        }

        private PropertyMetadata withField0(Class<?> owner, Field field, ResolvableType type) {
            Schema schema = this.getSchema(owner, field);
            String id = field.getName();

            SimplePropertyMetadata metadata = new SimplePropertyMetadata();
            metadata.setId(id);
            metadata.setName(id);
            metadata.setValueType(withType0(field, type));

            Map<String, Object> expands = new HashMap<>();
            // 在getter方法上定义的注解
            Method method = getReadMethod(owner, field);
            if (method != null) {
                parseExpands(method, true, expands);
            }
            // 在字段上定义的注解
            parseExpands(field, true, expands);

            if (null != schema) {
                // 继承字段使用声明类的资源；getter 上的 Schema 覆盖字段定义。
                Class<?> declaringType = method != null && AnnotationUtils.findAnnotation(method, Schema.class) != null
                    ? method.getDeclaringClass()
                    : field.getDeclaringClass();
                // 先设置扩展，延迟国际化可在同一模型中保留资源键。
                metadata.setExpands(expands);
                describe(metadata, schema, List.of(declaringType.getName() + "." + id), false);
            }
            metadata.setExpands(expands);

            return metadata;

        }

        private DataType withType0(Object owner, ResolvableType type) {
            Class<?> clazz = type.toClass();
            if (clazz == Object.class) {
                return null;
            }
            if (Publisher.class.isAssignableFrom(clazz)) {
                clazz = type.getGeneric(0).toClass();
            }
            if (Collection.class.isAssignableFrom(clazz)) {
                ArrayType arrayType = new ArrayType();
                arrayType.setElementType(withType0(owner, type.getGeneric(0)));
                return arrayType;
            }
            if (clazz.isArray()) {
                ArrayType arrayType = new ArrayType();
                arrayType.setElementType(withType0(owner, ResolvableType.forType(clazz.getComponentType())));
                return arrayType;
            }
            if (Map.class.isAssignableFrom(clazz)) {
                return new ObjectType();
            }
            if (clazz == String.class || clazz == Character.class || CharSequence.class.isAssignableFrom(clazz)) {
                return new StringType();
            }
            if (clazz == byte.class || clazz == Byte.class) {
                return new IntType().max(Byte.MAX_VALUE);
            }
            if (clazz == short.class || clazz == Short.class) {
                return new IntType().max(Short.MAX_VALUE).min(0);
            }
            if (clazz == int.class || clazz == Integer.class) {
                return new IntType();
            }
            if (clazz == long.class || clazz == Long.class || clazz == BigInteger.class) {
                return new LongType();
            }
            if (clazz == float.class || clazz == Float.class) {
                return new FloatType();
            }
            if (clazz == Boolean.class || clazz == boolean.class || clazz == AtomicBoolean.class) {
                return new BooleanType();
            }

            // 其他数字类型默认double
            if (Number.class.isAssignableFrom(clazz)) {
                return new DoubleType();
            }

            // 时间类型
            if (DateTimeType.isSupported(clazz)) {
                return DateTimeType.fromJavaType(clazz);
            }

            if (clazz.isEnum()) {
                EnumType enumType = new EnumType();
                Map<String, List<String>> keys = new LinkedHashMap<>();
                for (Object constant : clazz.getEnumConstants()) {
                    if (constant instanceof EnumDict) {
                        EnumDict<?> dict = ((EnumDict<?>) constant);
                        enumType.addElement(EnumType.Element.of(String.valueOf(dict.getValue()), dict.getText()));
                        if (!resolveMessages) {
                            keys.put(String.valueOf(dict.getValue()), List.of(dict.getI18nCode()));
                        }
                    } else {
                        Enum<?> dict = ((Enum<?>) constant);
                        enumType.addElement(EnumType.Element.of(dict.name(), dict.name()));
                    }
                }
                if (!keys.isEmpty()) {
                    enumType.expand(I18N_KEYS, keys);
                }
                return enumType;
            }

            ObjectType objectType = new ObjectType();
            Class<?> fClass = clazz;
            Map<String, Integer> indexMap = new HashMap<>();
            ReflectionUtils.doWithFields(fClass, field -> {
                Order order = AnnotationUtils.findAnnotation(field, Order.class);

                if (owner != null && !distinct.add(Tuples.of(owner, field))) {
                    PropertyMetadata propertyMetadata = withField0(fClass, field, ResolvableType.forClass(Map.class));
                    if (order != null) {
                        indexMap.put(propertyMetadata.getId(), order.value());
                    }
                    objectType.addPropertyMetadata(propertyMetadata);
                    return;
                }
                Schema schema = getSchema(fClass, field);
                if ((schema == null || schema.hidden()) && hasSemanticDeclaration(field, getReadMethod(fClass, field))) {
                    throw new I18nSupportException.NoStackTrace(
                        "error.invalid_metadata_semantics", fClass.getName() + "." + field.getName() + ": @Schema");
                }
                if (schema != null && !schema.hidden()) {
                    PropertyMetadata propertyMetadata = withField0(fClass, field, ResolvableType.forField(field, type));
                    if (order != null) {
                        indexMap.put(propertyMetadata.getId(), order.value());
                    }
                    objectType.addPropertyMetadata(propertyMetadata);
                }
            });
            objectType
                .getProperties()
                .sort(Comparator.comparingLong(m -> indexMap
                    .getOrDefault(m.getId(), Objects.equals(m.getId(), "id") ? Integer.MIN_VALUE : Integer.MAX_VALUE)));
            objectType.setExpands(MetadataUtils.parseExpands(fClass, expandsMerge));
            MetadataSemantics.validateOutput(objectType);
            return objectType;
        }

        /** Semantic markers do not silently opt otherwise private fields into a public schema. */
        private boolean hasSemanticDeclaration(AnnotatedElement... elements) {
            for (AnnotatedElement element : elements) {
                if (element != null && (AnnotatedElementUtils.hasAnnotation(element, GraphSubject.class)
                    || AnnotatedElementUtils.hasAnnotation(element, SemanticRole.class)
                    || AnnotatedElementUtils.hasAnnotation(element, Presentation.class))) {
                    return true;
                }
            }
            return false;
        }

        private Method getReadMethod(Class<?> owner, Field field) {
            String name = field.getName();
            try {
                PropertyDescriptor descriptor = new PropertyDescriptor(name, owner);
                return descriptor.getReadMethod();
            } catch (IntrospectionException ignore) {

            }
            return null;
        }

        private Schema getSchema(Class<?> owner, Field field) {
            return getAnnotation(owner, field, Schema.class);
        }

        private <T extends Annotation> T getAnnotation(Class<?> owner, Field field, Class<T> annotation) {
            Method readMethod = getReadMethod(owner, field);
            if (readMethod != null) {
                T schema = AnnotatedElementUtils.getMergedAnnotation(readMethod, annotation);
                if (schema != null) {
                    return schema;
                }
            }
            return AnnotatedElementUtils.getMergedAnnotation(field, annotation);
        }
    }
}
