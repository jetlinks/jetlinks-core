package org.jetlinks.core.command;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.jetlinks.core.metadata.DataType;
import org.jetlinks.core.metadata.FunctionMetadata;
import org.jetlinks.core.metadata.PropertyMetadata;
import org.jetlinks.core.metadata.types.ObjectType;
import org.jetlinks.core.metadata.types.StringType;
import org.junit.Test;
import org.springframework.core.ResolvableType;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.junit.Assert.*;

public class CommandMetadataResolverTest {


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


    @Schema(title = "测试1", description = "测试1.")
    public static class Test1Command extends AbstractCommand<Mono<String>, Test1Command> {

        @Schema(description = "Str")
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

}