package org.jetlinks.core.utils;

import io.swagger.v3.oas.annotations.media.Schema;
import org.hswebframework.web.dict.I18nEnumDict;
import org.hswebframework.web.i18n.LocaleUtils;
import org.jetlinks.core.annotation.Attr;
import org.jetlinks.core.annotation.metric.Counter;
import org.jetlinks.core.annotation.metric.Gauge;
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
        assertEquals("Samples", MetadataUtils.localize(property, Locale.ENGLISH, source).getName());
        Map<?, ?> customized = (Map<?, ?>) MetadataUtils.parseExpands(Customized.class.getMethod("getReceived")).get("metric");
        assertEquals("seconds", customized.get("unit"));
    }

    @Test
    public void definitionsAreIndependentOfTheFirstCallersLocaleAndViewsAreIsolated() throws Exception {
        ObjectType definition = LocaleUtils.doWith(Locale.ENGLISH,
            () -> MetadataUtils.parseReadOnlyType(ResolvableType.forClass(Values.class)));
        assertEquals("当前值", definition.getProperty("value").orElseThrow().getName());
        for (int i = 0; i < 10; i++) {
            var english = CompletableFuture.supplyAsync(() -> MetadataUtils.localize(definition, Locale.ENGLISH, source));
            var chinese = CompletableFuture.supplyAsync(() -> MetadataUtils.localize(definition, Locale.CHINESE, source));
            assertEquals("Current value", english.get().getProperty("value").orElseThrow().getName());
            assertEquals("当前值", chinese.get().getProperty("value").orElseThrow().getName());
        }
        ObjectType english = MetadataUtils.localize(definition, Locale.ENGLISH, source);
        english.getProperty("value").orElseThrow().getExpands().clear();
        assertFalse(definition.getProperty("value").orElseThrow().getExpands().isEmpty());
        assertEquals("当前值", definition.getProperty("value").orElseThrow().getName());
    }

    @Test
    public void nestedPropertiesAndEnumLabelsAreLocalizedWithoutMutatingDefinitions() {
        ObjectType definition = MetadataUtils.parseReadOnlyType(ResolvableType.forClass(Values.class));
        ObjectType english = MetadataUtils.localize(definition, Locale.ENGLISH, source);
        EnumType state = (EnumType) english.getProperty("status").orElseThrow().getValueType();
        assertEquals("Running", state.getElements().get(0).getText());
        EnumType original = (EnumType) definition.getProperty("status").orElseThrow().getValueType();
        assertEquals("运行中", original.getElements().get(0).getText());
        assertEquals("Nested value", ((ObjectType) english.getProperty("nested").orElseThrow().getValueType())
            .getProperty("name").orElseThrow().getName());
        assertEquals("运行中", ((EnumType) MetadataUtils.localize(definition, Locale.JAPANESE)
            .getProperty("status").orElseThrow().getValueType()).getElements().get(0).getText());
    }

    @Test
    public void bareSemanticAnnotationsCanUsePropertyResourceKeysWithoutSchemaBoilerplate() {
        ObjectType definition = MetadataUtils.parseReadOnlyType(ResolvableType.forClass(Bare.class));
        assertEquals("received", definition.getProperty("received").orElseThrow().getName());
        assertEquals("Received", MetadataUtils.localize(definition, Locale.ENGLISH, source)
            .getProperty("received").orElseThrow().getName());
    }

    @Test
    public void embeddedMessagesHavePriorityOverResourceKeys() throws Exception {
        PropertyMetadata definition = MetadataUtils.parsePropertyDefinition(
            ResolvableType.forClass(Values.class), Values.class.getMethod("getReceived"));
        definition.getExpands().put("i18nMessages", Map.of("name", Map.of("en", "Embedded", "zh_CN", "内嵌")));
        assertEquals("Embedded", MetadataUtils.localize(definition, Locale.UK).getName());
        assertEquals("内嵌", MetadataUtils.localize(definition, Locale.SIMPLIFIED_CHINESE).getName());
        assertEquals("接收样本", MetadataUtils.localize(definition, Locale.JAPANESE).getName());
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
