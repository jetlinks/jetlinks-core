package org.jetlinks.core.command;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.hswebframework.web.bean.FastBeanCopier;
import org.hswebframework.web.i18n.LocaleUtils;
import org.hswebframework.web.i18n.MessageSourceInitializer;
import org.jetlinks.core.annotation.Expands;
import org.jetlinks.core.annotation.command.Unbounded;
import org.jetlinks.core.annotation.ui.Selector;
import org.jetlinks.core.metadata.DataType;
import org.jetlinks.core.metadata.FunctionMetadata;
import org.jetlinks.core.metadata.PropertyMetadata;
import org.jetlinks.core.metadata.types.ArrayType;
import org.jetlinks.core.metadata.types.ObjectType;
import org.jetlinks.core.metadata.types.StringType;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.Order;
import reactor.core.publisher.Mono;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import static org.junit.Assert.*;

public class CommandMetadataResolverTest {

    @BeforeClass
    public static void messages() {
        StaticMessageSource source = new StaticMessageSource();
        source.addMessage(LocalizedEntity.class.getName() + ".name", Locale.ENGLISH, "Device name description");
        source.addMessage(LocalizedEntity.class.getName() + ".name.title", Locale.ENGLISH, "Device name");
        source.addMessage(LocalizedEntity.class.getName() + ".id", Locale.ENGLISH, "Device ID");
        source.addMessage(LocalizedEntity.class.getName() + ".titleOnly.title", Locale.ENGLISH, "Title only");
        source.addMessage(LocalizedBaseEntity.class.getName() + ".inherited", Locale.ENGLISH, "Inherited field");
        source.addMessage(LocalizedChildEntity.class.getName() + ".value", Locale.ENGLISH, "Nested value");
        source.addMessage(LocalizedCommand.class.getName() + ".target", Locale.ENGLISH, "Target description");
        source.addMessage(LocalizedCommand.class.getName() + ".target.title", Locale.ENGLISH, "Target");
        MessageSourceInitializer.init(source);
    }

    @Test
    public void testEntityFieldMessagesPreserveInheritanceAndGetterSchema() {
        LocaleUtils.doWith(Locale.ENGLISH, ignored -> {
            ObjectType type = (ObjectType) CommandMetadataResolver.resolveOutput(ResolvableType.forClass(LocalizedEntity.class));
            PropertyMetadata name = type.getProperty("name").orElseThrow();
            assertEquals("Device name", name.getName());
            assertEquals("Device name description", name.getDescription());
            assertTrue(name.getValueType() instanceof StringType);
            assertEquals("Device ID", type.getProperty("id").orElseThrow().getDescription());
            assertEquals("Inherited field", type.getProperty("inherited").orElseThrow().getDescription());
            PropertyMetadata titleOnly = type.getProperty("titleOnly").orElseThrow();
            assertEquals("Title only", titleOnly.getName());
            assertNull(titleOnly.getDescription());
            ObjectType nested = (ObjectType) type.getProperty("nested").orElseThrow().getValueType();
            assertEquals("Nested value", nested.getProperty("value").orElseThrow().getDescription());
            assertFalse(type.getProperty("hidden").isPresent());
            assertFalse(type.getProperty("unannotated").isPresent());
            assertEquals(type.getProperties().size(), CommandMetadataResolver.resolveInputs(ResolvableType.forClass(LocalizedEntity.class)).size());
        });
    }

    @Test
    public void testMissingFieldMessagesKeepOriginalSchemaText() {
        for (Locale locale : List.of(Locale.CHINESE, Locale.FRENCH)) {
            LocaleUtils.doWith(locale, ignored -> {
                ObjectType type = (ObjectType) CommandMetadataResolver.resolveOutput(ResolvableType.forClass(LocalizedEntity.class));
                PropertyMetadata name = type.getProperty("name").orElseThrow();
                assertEquals("默认标题", name.getName());
                assertEquals("默认说明", name.getDescription());
                assertEquals("设备ID", type.getProperty("id").orElseThrow().getDescription());
                assertEquals("继承字段", type.getProperty("inherited").orElseThrow().getDescription());
                PropertyMetadata titleOnly = type.getProperty("titleOnly").orElseThrow();
                assertEquals("仅标题", titleOnly.getName());
                assertNull(titleOnly.getDescription());
            });
        }
    }

    @Test
    public void testCommandGetterMessagesUseSchemaNameAndDeclaringClass() {
        for (Class<?> commandType : List.of(LocalizedCommand.class, LocalizedChildCommand.class)) {
            for (Locale locale : List.of(Locale.ENGLISH, Locale.CHINESE, Locale.ENGLISH)) {
                LocaleUtils.doWith(locale, ignored -> {
                    List<PropertyMetadata> inputs = CommandMetadataResolver.resolveInputs(ResolvableType.forClass(commandType));
                    assertEquals(1, inputs.size());
                    PropertyMetadata input = inputs.get(0);
                    assertEquals("target", input.getId());
                    assertEquals(locale.equals(Locale.ENGLISH) ? "Target" : "目标", input.getName());
                    assertEquals(locale.equals(Locale.ENGLISH) ? "Target description" : "目标说明", input.getDescription());
                    assertTrue(input.getValueType() instanceof StringType);
                });
            }
        }
    }

    @Getter
    @Setter
    public static class LocalizedBaseEntity {
        private String id;

        @Schema(description = "继承字段")
        private String inherited;
    }

    @Getter
    @Setter
    public static class LocalizedEntity extends LocalizedBaseEntity {
        @Schema(title = "默认标题", description = "默认说明")
        private String name;

        @Schema(title = "仅标题")
        private String titleOnly;

        @Schema(description = "嵌套对象")
        private LocalizedChildEntity nested;

        @Schema(hidden = true)
        private String hidden;

        private String unannotated;

        @Override
        @Schema(description = "设备ID")
        public String getId() {
            return super.getId();
        }
    }

    public static class LocalizedChildEntity {
        @Schema(description = "嵌套值")
        private String value;
    }

    public static class LocalizedCommand extends AbstractCommand<Mono<String>, LocalizedCommand> {
        @Schema(name = "target", title = "目标", description = "目标说明")
        public String getValue() {
            return getOrNull("target", String.class);
        }
    }

    public static class LocalizedChildCommand extends LocalizedCommand {
    }


    @Test
    public void testSimple() {
        FunctionMetadata metadata = CommandMetadataResolver.resolve(ResolvableType.forClass(Test1Command.class));
        System.out.println(JSON.toJSONString(metadata.toJson(), SerializerFeature.PrettyFormat));
        assertNotNull(metadata);

        assertEquals("Test1", metadata.getId());
        assertEquals("测试1", metadata.getName());
        assertEquals("测试1.", metadata.getDescription());
        assertNotNull(metadata.getInputs());
        assertNotNull(metadata.getOutput());
        assertTrue(metadata.getOutput() instanceof StringType);
    }

    @Test
    public void testClass() {
        List<PropertyMetadata> inputs = CommandMetadataResolver.resolveInputs(ResolvableType.forClass(Test2.class));
        System.out.println(JSON.toJSONString(inputs, SerializerFeature.PrettyFormat));
        assertNotNull(inputs);
        assertEquals(2, inputs.size());

        DataType output = CommandMetadataResolver.resolveOutput(ResolvableType.forClass(Test2.class));
        System.out.println(JSON.toJSONString(output, SerializerFeature.PrettyFormat));
        assertNotNull(output);
        assertTrue(output instanceof ObjectType);
        assertEquals(2, ((ObjectType) output).getProperties().size());
    }

    @Test
    public void testInner() {
        List<PropertyMetadata> inputs = CommandMetadataResolver.resolveInputs(ResolvableType.forClass(TestInnerCommand.class));
        assertNotNull(inputs);

        assertEquals(2, inputs.size());
    }

    @Test
    public void testGeneric() {

        FunctionMetadata metadata = CommandMetadataResolver.resolve(
            ResolvableType.forClassWithGenerics(TestGenericCommand.class, String.class, Test2.class)
        );
        System.out.println(JSON.toJSONString(metadata.toJson(),SerializerFeature.PrettyFormat));
        {
            List<PropertyMetadata> inputs = metadata.getInputs();


            assertEquals(1, inputs.size());
            PropertyMetadata prop = inputs.get(0);
            assertTrue(prop.getValueType() instanceof ArrayType);
            ObjectType type = (ObjectType) ((ArrayType) prop.getValueType()).getElementType();
            System.out.println(type.getProperties());

        }
    }

    @Test
    public void testGenericSub() {
        List<PropertyMetadata> inputs = CommandMetadataResolver.resolveInputs(
            ResolvableType.forClassWithGenerics(TestGenericCommandSub.class, String.class, Test2.class)
        );
        System.out.println(inputs);
        assertEquals(1, inputs.size());
        PropertyMetadata metadata = inputs.get(0);
        assertTrue(metadata.getValueType() instanceof ArrayType);

        ObjectType type = (ObjectType) ((ArrayType) metadata.getValueType()).getElementType();
        System.out.println(type.getProperties());
    }

    @Test
    public void testGenericNoSpec() {

        List<PropertyMetadata> inputs = CommandMetadataResolver.resolveInputs(
            ResolvableType.forClassWithGenerics(TestGenericCommandNoSpec.class, String.class, Test2.class)
        );
        System.out.println(inputs);
        assertEquals(2, inputs.size());


    }

    public static class TestGenericCommandNoSpec<R, T>
        extends AbstractCommand<R, TestGenericCommandNoSpec<R, T>> implements GenericInputCommand<T> {


    }

    public static class TestGenericCommandSub<R, T> extends TestGenericCommand<R, T>
        implements GenericInputCommand<T> {

    }

    public static class TestGenericCommand<R, T> extends AbstractCommand<Mono<R>, TestGenericCommand<R, T>>
        implements GenericInputCommand<T>{

        @Getter
        @Setter
        static class InputSpec<T> implements GenericInputCommand.InputSpec<T> {
            @Schema(title = "Data")
            private List<T> data;
        }
    }


    @Schema(title = "测试2", description = "测试2.")
    public static class TestInnerCommand extends AbstractCommand<Mono<String>, Test1Command> {


        static class InputSpec extends Test2 {

        }

    }

    @Schema(title = "测试1", description = "测试1.")
    public static class Test1Command extends AbstractCommand<Mono<String>, Test1Command> {

        @Schema(description = "Str")
        @Selector(type = "device")
        public String getStr() {
            return getOrNull("str", String.class);
        }

        @Schema(description = "Index")
        public Integer getIndex() {
            return getOrNull("index", Integer.class);
        }

    }

    @Getter
    @Setter
    public static class Test2 {

        @Schema(description = "名称")
        private String name;

        @Schema(description = "数量")
        private Long number;

        private String ignore;

    }

    @Target({ElementType.FIELD, ElementType.METHOD, ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @Expands(key = "testAnnotation")
    public @interface TestClassAnnotation {

        Class<?> innerClass() default Object.class;

    }

    @TestClassAnnotation(innerClass = Test2.class)
    @Schema(title = "测试2", description = "测试2.")
    @Unbounded
    public static class Test2Command extends AbstractCommand<Mono<String>, Test2Command> {

        @Schema(description = "Str")
        public String getStr() {
            return getOrNull("str", String.class);
        }

        @Schema(description = "Index")
        public Integer getIndex() {
            return getOrNull("index", Integer.class);
        }

    }

    @Test
    public void testClassAnnotation() {
        FunctionMetadata metadata = CommandMetadataResolver.resolve(Test2Command.class);
        System.out.println(JSON.toJSONString(metadata.toJson(), SerializerFeature.PrettyFormat));
        assertNotNull(metadata);

        Object testAnnotation = metadata.getExpands().get("testAnnotation");
        assertNotNull(testAnnotation);
        Map<String, Object> copy = FastBeanCopier.copy(testAnnotation, new HashMap<>());

        Object innerClass = copy.get("innerClass");
        assertNotNull(innerClass);
        List<PropertyMetadata> metadataList = ((List<PropertyMetadata>) innerClass);
        assertEquals(2, metadataList.size());
        assertEquals("name", metadataList.get(0).getId());
        assertEquals("number", metadataList.get(1).getId());


    }

    public static class Test3Command extends Test1Command {

        @Order(1)
        @Schema(description = "Str", hidden = true)
        @Selector(type = "device")
        public String getStr() {
            return super.getStr();
        }

        @Schema(description = "Index")
        public Integer getIndex() {
            return super.getIndex();
        }

    }

    @Test
    public void testHiddenParentMethod() {
        FunctionMetadata resolve = CommandMetadataResolver.resolve(Test3Command.class);
        assertNotNull(resolve.getInputs());
        assertEquals(2, resolve.getInputs().size());
        Optional<Object> hidden = resolve
            .getInputs()
            .stream()
            .filter(p->p.getId().equals("str"))
            .findAny()
            .flatMap(p->p.getExpand("hidden"))
           ;
        assertTrue(hidden.isPresent());
        assertTrue(((boolean) hidden.get()));
    }

}
