package org.jetlinks.core.metadata;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.hswebframework.web.exception.I18nSupportException;
import org.junit.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.Assert.*;

/** 覆盖构造式模型的完整目录往返、空输入及非法条目的替换边界。 */
public class SimpleDeviceMetadataTest {

    @Test
    public void should_roundtrip_all_directories_and_data_types() {
        SimpleDeviceMetadata model = model();
        assertEquals("string", model.getPropertyOrNull("value").getValueType().getId());
        assertEquals("string", model.getTagOrNull("tag").getValueType().getId());
        assertEquals("boolean", model.getFunctionOrNull("read").getOutput().getId());
        assertEquals("string", model.getFunctionOrNull("read").getInputs().get(0).getValueType().getId());
        assertTrue(model.getFunctionOrNull("read").isAsync());
        assertEquals("string", model.getEventOrNull("changed").getType().getId());
        assertEquals("device", model.getRelationMetadata().get(0).getTargetType());
        assertEquals("string", model.getModuleOrNull("nested").getPropertyOrNull("nestedValue").getValueType().getId());

        SimpleDeviceMetadata decoded = new SimpleDeviceMetadata();
        decoded.fromJson(JSON.parseObject(model.toJson().toJSONString()));
        assertEquals(model.toJson(), decoded.toJson());
        decoded.addProperty(SimplePropertyMetadata.of("added", "Added", null));
        assertNotNull(decoded.getPropertyOrNull("added"));
    }

    @Test
    public void should_replace_all_state_with_empty_model() {
        for (JSONObject empty : new JSONObject[]{null, new JSONObject()}) {
            SimpleDeviceMetadata model = model();
            model.fromJson(empty);
            assertNull(model.getId());
            assertNull(model.getName());
            assertNull(model.getDescription());
            assertNull(model.getExpands());
            assertEquals(new SimpleDeviceMetadata().toJson(), model.toJson());
            model.addFunction(new SimpleFunctionMetadata());
            model.addEvent(new SimpleEventMetadata());
            model.addTag(new SimplePropertyMetadata());
        }
    }

    @Test
    public void should_reject_invalid_entries_without_replacing_existing_model() {
        String[] directories = {"properties", "functions", "events", "tags", "relations", "modules"};
        String[] invalidEntries = {"null", "7", "true", "\"invalid\"", "[]"};
        for (String directory : directories) {
            for (String entry : invalidEntries) {
                SimpleDeviceMetadata model = model();
                JSONObject before = model.toJson();
                JSONObject invalid = JSON.parseObject("{\"" + directory + "\":[" + entry + "]}");
                assertInvalidDefinition(() -> model.fromJson(invalid));
                assertEquals(before, model.toJson());
            }
        }
        assertInvalidDefinition(() -> new RelationMetadata().fromJson(null));
    }

    @Test
    public void should_preserve_optional_empty_directories() {
        SimpleDeviceMetadata model = new SimpleDeviceMetadata();
        model.fromJson(JSON.parseObject("{\"id\":\"legacy\",\"properties\":null,\"relations\":null,\"modules\":null}"));
        assertEquals("legacy", model.getId());
        assertTrue(model.getProperties().isEmpty());
        assertFalse(model.toJson().containsKey("relations"));
        assertFalse(model.toJson().containsKey("modules"));
    }

    @Test
    public void should_reject_invalid_nested_object_properties() {
        JSONObject invalid = JSON.parseObject("{\"properties\":[{\"id\":\"object\","
            + "\"valueType\":{\"type\":\"object\",\"properties\":[null]}}]}");
        assertInvalidDefinition(() -> new SimpleDeviceMetadata().fromJson(invalid));
    }

    @Test
    public void should_replace_function_inputs_and_optional_output() {
        SimpleFunctionMetadata function = new SimpleFunctionMetadata();
        function.fromJson(model().getFunctionOrNull("read").toJson());
        function.fromJson(JSON.parseObject("{\"id\":\"void\"}"));
        assertEquals("void", function.getId());
        assertTrue(function.getInputs().isEmpty());
        assertNull(function.getOutput());
        assertNull(function.toJson().getJSONObject("output"));
    }

    @Test
    public void should_reject_invalid_function_inputs() {
        SimpleFunctionMetadata function = new SimpleFunctionMetadata();
        assertInvalidDefinition(() -> function.fromJson(JSON.parseObject("{\"inputs\":[null]}")));
    }

    @Test
    public void should_replace_optional_event_type() {
        SimpleEventMetadata event = new SimpleEventMetadata();
        event.fromJson(model().getEventOrNull("changed").toJson());
        event.fromJson(JSON.parseObject("{\"id\":\"unknown\"}"));
        assertEquals("unknown", event.getType().getId());
        assertEquals("unknown", event.toJson().getJSONObject("valueType").getString("type"));
    }

    private static void assertInvalidDefinition(Runnable action) {
        StepVerifier.create(Mono.fromRunnable(action))
                    .expectError(I18nSupportException.class)
                    .verify();
    }

    private static SimpleDeviceMetadata model() {
        String property = "{\"id\":\"value\",\"valueType\":{\"type\":\"string\"}}";
        JSONObject json = JSON.parseObject("{\"id\":\"model\",\"name\":\"Model\",\"description\":\"Description\","
            + "\"expands\":{\"custom\":true},\"properties\":[" + property + "],"
            + "\"tags\":[{\"id\":\"tag\",\"valueType\":{\"type\":\"string\"}}],"
            + "\"functions\":[{\"id\":\"read\",\"async\":true,\"inputs\":[" + property + "],\"output\":{\"type\":\"boolean\"}}],"
            + "\"events\":[{\"id\":\"changed\",\"valueType\":{\"type\":\"string\"}}],"
            + "\"relations\":[{\"id\":\"about\",\"targetType\":\"device\"}],"
            + "\"modules\":[{\"id\":\"nested\",\"properties\":[{\"id\":\"nestedValue\",\"valueType\":{\"type\":\"string\"}}]}]}");
        SimpleDeviceMetadata model = new SimpleDeviceMetadata();
        model.fromJson(json);
        return model;
    }
}
