package org.jetlinks.core.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.v3.oas.annotations.ExternalDocumentation;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;
import org.hswebframework.web.validator.CreateGroup;
import org.jetlinks.core.JsonViews;
import org.jetlinks.core.annotation.Attr;
import org.jetlinks.core.annotation.Expands;
import org.jetlinks.core.annotation.ui.Selector;
import org.jetlinks.core.annotation.ui.FormField;
import org.jetlinks.core.annotation.command.ModuleEventListener;
import org.jetlinks.core.metadata.DataType;
import org.jetlinks.core.metadata.ExpandsMerge;
import org.jetlinks.core.metadata.types.DateTimeType;
import org.jetlinks.core.metadata.types.ObjectType;
import org.jetlinks.core.utils.json.ObjectMappers;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.lang.annotation.ElementType.*;

public class MetadataUtilsTest {


    @Test
    public void testParseExpands() throws Exception {
        var field = CustomExpands.class.getDeclaredField("value");
        Assert.assertEquals(Map.of("caption", "second"), MetadataUtils.parseExpands(field).get("card"));

        var rules = ExpandsMerge.object(Map.of("card", ExpandsMerge.object(Map.of())));
        Assert.assertEquals(Map.of("caption", "first", "extra", "kept"),
                            MetadataUtils.parseExpands(field, rules).get("card"));
        var strict = ExpandsMerge.object(Map.of("card", ExpandsMerge.object(Map.of(
            "caption", ExpandsMerge.requireEqual(java.util.Objects::deepEquals,
                                                 () -> new IllegalArgumentException("caption"))))));
        reactor.test.StepVerifier.create(reactor.core.publisher.Mono.fromRunnable(
            () -> MetadataUtils.parseExpands(field, strict)))
            .expectError(IllegalArgumentException.class).verify();
    }

    @Test
    public void ordinaryGetterAndFieldNamespacesKeepTheirExistingPrecedence() {
        ObjectType type = (ObjectType) MetadataUtils.parseType(ResolvableType.forClass(NamespacePrecedence.class));
        var expands = type.getProperty("value").orElseThrow().getExpands();
        Assert.assertEquals(Map.of("type", "field", "multiple", false), expands.get("selector"));
        Assert.assertEquals(Map.of("label", "field", "hidden", false, "ignore", false), expands.get("form"));
        Assert.assertEquals(Map.of("primary", "field"), expands.get("ordinary"));
        Assert.assertEquals("retained", expands.get("getterOnly"));
    }

    @Test
    public void eventConfigurationRetainsTheDeclaredArrayAndSequence() throws Exception {
        var field = EventOptions.class.getMethod("listen");
        var parsed = (Map<?, ?>) MetadataUtils.parseExpands(field).get("eventListener");
        Assert.assertArrayEquals(new String[]{"second", "first", "second"}, (String[]) parsed.get("events"));
    }

    public static class CustomExpands {
        @Expands(key = "card", value = {@Attr(key = "caption", value = "first"), @Attr(key = "extra", value = "kept")})
        @Expands(key = "card", value = @Attr(key = "caption", value = "second"))
        private String value;
    }

    public static class NamespacePrecedence {
        @Schema @Selector(type = "field") @FormField(label = "field")
        @Expands(key = "ordinary", value = @Attr(key = "primary", value = "field"))
        private String value;

        @Selector(type = "getter", multiple = true) @FormField(label = "getter", hidden = true)
        @Expands(key = "ordinary", value = {@Attr(key = "primary", value = "getter"), @Attr(key = "extra", value = "discarded")})
        @Expands(@Attr(key = "getterOnly", value = "retained"))
        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }
    }

    public static class EventOptions {
        @ModuleEventListener(events = {"second", "first", "second"})
        public void listen() { }
    }

    @Test
    public void testAttr() {

        ObjectType type = (ObjectType) MetadataUtils.parseType(ResolvableType.forType(TestEntity.class));
        Map<?, ?> selector = (Map<?, ?>) type.getProperty("deviceId").orElseThrow().getExpands().get(Selector.KEY);
        Assert.assertEquals("device", selector.get("type"));
        Assert.assertEquals(true, selector.get("multiple"));
        Assert.assertTrue(selector.get("dataType") instanceof ObjectType);
        Assert.assertEquals("v1", type.getProperty("productId").orElseThrow().getExpands().get("k1"));

    }

    @Test
    public void testCollectionParse() {
        ObjectType type = (ObjectType) MetadataUtils.parseType(ResolvableType.forType(CollectionsTest.class));

        System.out.println(JSON.toJSONString(type, SerializerFeature.PrettyFormat));
    }

    @Test
    public void testDateTimeType() {
        DataType dataType = MetadataUtils.parseType(ResolvableType.forType(Timestamp.class));

        Assert.assertTrue(dataType instanceof DateTimeType);
    }

    @Test
    public void testJsr303Required() {
        ObjectType type = (ObjectType) MetadataUtils.parseType(ResolvableType.forType(RequiredEntity.class));

        Assert.assertTrue(type
                              .getProperty("deviceId")
                              .orElseThrow()
                              .getExpand("required")
                              .map(Boolean.TRUE::equals)
                              .orElse(false));
        Assert.assertFalse(type
                               .getProperty("productId")
                               .orElseThrow()
                               .getExpand("required")
                               .map(Boolean.TRUE::equals)
                               .orElse(false));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testSwaggerExternalDocsAndExtensions() throws Exception {
        ObjectType type = (ObjectType) MetadataUtils.parseType(ResolvableType.forType(SwaggerEntity.class));

        Map<String, Object> expands = type
            .getProperty("document")
            .orElseThrow()
            .getExpands();

        List<Map<String, Object>> externalDocs = (List<Map<String, Object>>) expands.get("externalDocs");
        Assert.assertEquals(1, externalDocs.size());
        Assert.assertEquals("工具帮助", externalDocs.get(0).get("description"));
        Assert.assertEquals("/tools/document_generate/help.md", externalDocs.get(0).get("url"));

        Map<String, Object> aiHelp = (Map<String, Object>) expands.get("x-ai-help");
        Assert.assertEquals("/tools/document_generate/help.md", aiHelp.get("path"));
        Assert.assertEquals("watermark", aiHelp.get("section"));

        Map<String, Object> options = (Map<String, Object>) expands.get("x-options");
        Assert.assertEquals(10, options.get("priority"));
        Assert.assertEquals(Map.of("enabled", true), options.get("flags"));

        Map<String, Object> methodExpands = MetadataUtils.parseExpands(SwaggerEntity.class.getMethod("generate"));
        List<Map<String, Object>> methodDocs = (List<Map<String, Object>>) methodExpands.get("externalDocs");
        Assert.assertEquals(1, methodDocs.size());
        Assert.assertEquals("方法工具帮助", methodDocs.get(0).get("description"));
        Assert.assertEquals("/tools/method_generate/help.md", methodDocs.get(0).get("url"));
        Map<String, Object> methodHelp = (Map<String, Object>) methodExpands.get("x-ai-help");
        Assert.assertEquals("/tools/method_generate/help.md", methodHelp.get("path"));
    }


    @Target({FIELD, METHOD, PARAMETER})
    @Retention(RetentionPolicy.RUNTIME)
    @Inherited
    @Selector(type = "device")
    public @interface DeviceSelector {

        @AliasFor(annotation = Selector.class)
        boolean multiple() default false;

        org.jetlinks.core.annotation.DataType dataType()
            default @org.jetlinks.core.annotation.DataType(
            CollectionsTest.class
        );

        CustomAnnotation custom() default @CustomAnnotation;

        CustomAnnotation[] customArr() default {
            @CustomAnnotation,
            @CustomAnnotation
        };
    }


    @Target({FIELD, METHOD, PARAMETER})
    @Retention(RetentionPolicy.RUNTIME)
    @Inherited
    public @interface CustomAnnotation {
        String x() default "1";

        String y() default "0";
    }

    @Getter
    @Setter
    public static class TestEntity {

        @Schema(title = "设备ID")
        @DeviceSelector(multiple = true, custom = @CustomAnnotation(x = "2"))
        @NotBlank(groups = CreateGroup.class)
        private String deviceId;

        @Schema(title = "产品ID")
        @Pattern(regexp = "^[0-9a-zA-Z_\\-]+$")
        public String productId;

        @Expands({@Attr(key = "k1", value = "v1"), @Attr(key = "k2", value = "v2")})
        @Expands(key = "test2", value = {
            @Attr(key = "k1", value = "v1"),
            @Attr(key = "k2", value = "v2")
        })
        @JsonView({JsonViews.Create.class,JsonViews.Detail.class})
        public String getProductId() {
            return productId;
        }
    }

    @Getter
    @Setter
    public static class CollectionsTest {


        @Schema(title = "set集合")
        private Set<String> set;

        @Schema(title = "list集合")
        private List<String> list;

        @Schema(title = "collection集合")
        private Collection<String> collection;
    }

    @Getter
    @Setter
    public static class RequiredEntity {

        @Schema(description = "设备ID")
        @NotBlank
        private String deviceId;

        @Schema(description = "产品ID")
        private String productId;
    }

    @Getter
    @Setter
    public static class SwaggerEntity {

        @Schema(
            title = "文档参数",
            externalDocs = @ExternalDocumentation(description = "工具帮助", url = "/tools/document_generate/help.md"),
            extensions = {
                @Extension(
                    name = "x-ai-help",
                    properties = {
                        @ExtensionProperty(name = "path", value = "/tools/document_generate/help.md"),
                        @ExtensionProperty(name = "section", value = "watermark")
                    }
                ),
                @Extension(
                    name = "x-options",
                    properties = {
                        @ExtensionProperty(name = "priority", value = "10", parseValue = true),
                        @ExtensionProperty(name = "flags", value = "{\"enabled\":true}", parseValue = true)
                    }
                )
            }
        )
        private String document;

        @ExternalDocumentation(description = "方法工具帮助", url = "/tools/method_generate/help.md")
        @Extension(name = "x-ai-help", properties = @ExtensionProperty(name = "path", value = "/tools/method_generate/help.md"))
        public void generate() {
        }
    }

}
