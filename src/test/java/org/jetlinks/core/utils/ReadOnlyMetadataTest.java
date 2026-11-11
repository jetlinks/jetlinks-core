package org.jetlinks.core.utils;

import io.swagger.v3.oas.annotations.media.Schema;
import org.hswebframework.web.dict.I18nEnumDict;
import org.hswebframework.web.i18n.LocaleUtils;
import org.jetlinks.core.annotation.Attr;
import org.jetlinks.core.annotation.metric.Counter;
import org.jetlinks.core.annotation.metric.Gauge;
import org.jetlinks.core.metadata.Metadata;
import org.jetlinks.core.metadata.PropertyMetadata;
import org.jetlinks.core.metadata.types.EnumType;
import org.jetlinks.core.metadata.types.LongType;
import org.jetlinks.core.metadata.types.ObjectType;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.*;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static java.lang.annotation.ElementType.*;
import static org.junit.Assert.*;

public class ReadOnlyMetadataTest {
    private static StaticMessageSource source;
    @BeforeClass
    public static void messages() {
        StaticMessageSource messages = new StaticMessageSource();
        messages.addMessage(Values.class.getName() + ".value.title", Locale.ENGLISH, "Current value");
        messages.addMessage(Values.class.getName() + ".value", Locale.ENGLISH, "Current reading");
        messages.addMessage(Samples.class.getName() + ".title", Locale.ENGLISH, "Samples");
        messages.addMessage(Status.class.getName() + ".running", Locale.ENGLISH, "Running");
        messages.addMessage(Nested.class.getName() + ".name.title", Locale.ENGLISH, "Nested value");
        messages.addMessage(Bare.class.getName() + ".received.title", Locale.ENGLISH, "Received");
        source = messages;
    }

    @Test
    public void explicitGetterModeResolvesInheritedGenericsAndKeepsFieldModeUnchanged() {
        ObjectType type = MetadataUtils.parseReadOnlyType(ResolvableType.forClass(Values.class));
        assertTrue(type.getProperty("value").orElseThrow().getValueType() instanceof LongType);
        assertTrue(type.getProperty("active").isPresent());
        assertTrue(type.getProperty("received").isPresent());
        assertEquals("active", type.getProperty("active").orElseThrow().getName());
        for (String excluded : List.of("hidden", "unannotated", "class", "staticValue", "wrong")) {
            assertFalse(excluded, type.getProperty(excluded).isPresent());
        }
        assertTrue(((ObjectType) MetadataUtils.parseType(ResolvableType.forClass(Values.class))).getProperties().isEmpty());
    }

    @Test
    public void nestedCompositionAndRenamedAliasUseTheSameExpansionParser() throws Exception {
        Map<?, ?> metric = (Map<?, ?>) MetadataUtils.parseExpands(Values.class.getMethod("getReceived")).get("metric");
        assertEquals("counter", metric.get("kind"));
        assertEquals("count", metric.get("unit"));
        PropertyMetadata property = MetadataUtils.parsePropertyDefinition(
            ResolvableType.forClass(Values.class), Values.class.getMethod("getReceived"));
        assertEquals("接收样本", property.getName());
        assertEquals("Samples", name(property, Locale.ENGLISH));
        Map<?, ?> customized = (Map<?, ?>) MetadataUtils.parseExpands(Customized.class.getMethod("getReceived")).get("metric");
        assertEquals("seconds", customized.get("unit"));
    }

    @Test
    public void concurrentTextResolutionDoesNotChangeSharedDefinitions() throws Exception {
        ObjectType definition = LocaleUtils.doWith(Locale.ENGLISH,
            () -> MetadataUtils.parseReadOnlyType(ResolvableType.forClass(Values.class)));
        PropertyMetadata property = definition.getProperty("value").orElseThrow();
        assertEquals("当前值", property.getName());
        for (int i = 0; i < 10; i++) {
            var english = CompletableFuture.supplyAsync(() -> name(property, Locale.ENGLISH));
            var chinese = CompletableFuture.supplyAsync(() -> name(property, Locale.CHINESE));
            assertEquals("Current value", english.get());
            assertEquals("当前值", chinese.get());
        }
        assertEquals("Current reading", MetadataUtils.resolveI18nMessage(
            Locale.ENGLISH, property, "description", property.getDescription(), source));
        assertEquals("当前值", property.getName());
        assertEquals("当前读取值", property.getDescription());
    }

    @Test
    public void nestedPropertiesAndEnumLabelsAreLocalizedWithoutMutatingDefinitions() {
        ObjectType definition = MetadataUtils.parseReadOnlyType(ResolvableType.forClass(Values.class));
        EnumType state = (EnumType) definition.getProperty("status").orElseThrow().getValueType();
        EnumType.Element element = state.getElements().get(0);
        assertEquals("Running", MetadataUtils.resolveI18nMessage(
            Locale.ENGLISH, state, element.getValue(), element.getText(), source));
        assertEquals("运行中", element.getText());
        assertEquals("Nested value", name(((ObjectType) definition.getProperty("nested").orElseThrow().getValueType())
            .getProperty("name").orElseThrow(), Locale.ENGLISH));
        assertEquals("运行中", MetadataUtils.resolveI18nMessage(
            Locale.JAPANESE, state, element.getValue(), element.getText(), source));
    }

    @Test
    public void bareSemanticAnnotationsCanUsePropertyResourceKeysWithoutSchemaBoilerplate() {
        ObjectType definition = MetadataUtils.parseReadOnlyType(ResolvableType.forClass(Bare.class));
        assertEquals("received", definition.getProperty("received").orElseThrow().getName());
        assertEquals("Received", name(definition.getProperty("received").orElseThrow(), Locale.ENGLISH));
    }

    @Test
    public void embeddedMessagesHavePriorityOverResourceKeys() throws Exception {
        PropertyMetadata definition = MetadataUtils.parsePropertyDefinition(
            ResolvableType.forClass(Values.class), Values.class.getMethod("getReceived"));
        definition.getExpands().put("i18nMessages", Map.of(
            "name", Map.of("en", "Embedded", "zh_CN", "内嵌"),
            "description", Map.of("en", "Embedded description")));
        assertEquals("Embedded", name(definition, Locale.ENGLISH));
        assertEquals("Embedded", definition.getI18nName(Locale.UK));
        assertEquals("内嵌", definition.getI18nName(Locale.SIMPLIFIED_CHINESE));
        assertEquals("接收样本", definition.getI18nName(Locale.JAPANESE));
        assertEquals("Embedded description", definition.getI18nDescription(Locale.UK));
        assertEquals("已接收的样本", definition.getI18nDescription(Locale.JAPANESE));
        assertEquals("Embedded description", LocaleUtils.doWith(Locale.UK, () -> definition.getI18nDescription()));
    }

    @Test
    public void covariantGettersUseTheMoreSpecificJavaType() {
        ObjectType definition = MetadataUtils.parseReadOnlyType(ResolvableType.forClass(Covariant.class));
        assertEquals(1, definition.getProperties().size());
        assertTrue(definition.getProperty("value").orElseThrow().getValueType() instanceof LongType);
    }

    private static String name(Metadata metadata, Locale locale) {
        return MetadataUtils.resolveI18nMessage(locale, metadata, "name", metadata.getName(), source);
    }

    public interface Generic<T> {
        @Schema(title = "当前值", description = "当前读取值")
        @Gauge
        T getValue();
    }

    public interface Values extends Generic<Long> {
        @Schema boolean isActive();
        @Samples long getReceived();
        @Schema Status getStatus();
        @Schema Nested getNested();
        @Schema(hidden = true) @Counter long getHidden();
        long getUnannotated();
        @Schema long getWrong(String input);
        @Schema static long getStaticValue() { return 1; }
    }

    public interface Customized {
        @Samples(measurement = "seconds") long getReceived();
    }

    public interface Bare { @Counter long getReceived(); }
    public interface AGeneral { @Gauge Number getValue(); }
    public interface ZSpecific { @Gauge Long getValue(); }
    public interface Covariant extends AGeneral, ZSpecific { }
    public static class Nested { @Schema(title = "嵌套值") private String name; }

    @Target({METHOD, ANNOTATION_TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @Counter
    @Schema(title = "接收样本", description = "已接收的样本")
    public @interface Samples {
        @AliasFor(annotation = Counter.class, attribute = "unit")
        @Attr(key = "unit", ignoreEmpty = true)
        String measurement() default "count";
    }

    public enum Status implements I18nEnumDict<String> {
        running;
        @Override public String getValue() { return name(); }
        @Override public String getText() { return "运行中"; }
    }
}
