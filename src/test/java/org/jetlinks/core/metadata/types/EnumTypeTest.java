package org.jetlinks.core.metadata.types;

import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import org.jetlinks.core.metadata.DefaultConfigMetadata;
import org.jetlinks.core.utils.SerializeUtils;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;
import java.util.Map;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class EnumTypeTest extends JsonableTestBase<EnumType> {

    @Override
    protected EnumType newInstance() {
        return new EnumType();
    }

    @Override
    protected void fillSampleData(EnumType instance) {
        // elements
        instance.addElement(EnumType.Element.of("1", "一", "第一"));
        instance.addElement(EnumType.Element.of("2", "二", "第二"));
        instance.addElement(EnumType.Element.of("3", "三", "第三"));

        // valueType -> 在 toJson 时会序列化为 {"type":"int",...}
        instance.setValueType(IntType.GLOBAL);

        // multi 标记
        instance.setMulti(true);
    }

    @Override
    protected void assertSampleData(EnumType instance) {
        assertNotNull(instance.getElements());
        assertEquals(3, instance.getElements().size());

        EnumType.Element e1 = instance.getElements().get(0);
        EnumType.Element e2 = instance.getElements().get(1);
        assertEquals("1", e1.getValue());
        assertEquals("一", e1.getText());
        assertEquals("第二", e2.getDescription()); // spot check

        assertTrue(instance.isMulti());

        assertNotNull(instance.getValueType());
        assertEquals("int", instance.getValueType().getId());
    }

    @Test
    public void test() {

        EnumType type = new EnumType();

        Assert.assertFalse(type.validate("1").isSuccess());
        assertEquals(type.format("1"), "1");

        type.addElement(EnumType.Element.of("1", "男"));
        type.addElement(EnumType.Element.of("2", "女"));

        Assert.assertTrue(type.validate("1").isSuccess());
        assertEquals(type.format("1"), "男");


    }

    @Test
    public void testMulti() {
        EnumType type = new EnumType();
        type.setMulti(true);

        type.addElement(EnumType.Element.of("1", "男"));
        type.addElement(EnumType.Element.of("2", "女"));

        Assert.assertTrue(type.validate("1,2").isSuccess());
        assertEquals(type.format("1,2"), "男,女");
        assertEquals(type.format(Lists.newArrayList("1", "2")), Lists.newArrayList("男", "女"));

        Assert.assertFalse(type.validate("3").isSuccess());
        Assert.assertTrue(type.validate("1,3").isSuccess());
        Assert.assertFalse(type.validate("3,4").isSuccess());
        assertEquals(type.format("1,3"), "男,3");
        assertEquals(type.format(Lists.newArrayList("1", "3")), Lists.newArrayList("男", "3"));
    }


    @Test
    public void testToJsonWithoutElementsShouldKeepConfiguration() {
        for (boolean multi : new boolean[]{false, true}) {
            EnumType type = new EnumType();
            type.setValueType(StringType.GLOBAL);
            type.setMulti(multi);

            JSONObject json = type.toJson();

            Assert.assertFalse(json.containsKey("elements"));
            assertNotNull(json.getJSONObject("valueType"));
            assertEquals(StringType.ID, json
                .getJSONObject("valueType")
                .getString("type"));
            assertEquals(Boolean.valueOf(multi), json.getBoolean("multi"));

            EnumType restored = new EnumType();
            restored.fromJson(json);
            Assert.assertNull(restored.getElements());
            assertNotNull(restored.getValueType());
            assertEquals(StringType.ID, restored
                .getValueType()
                .getId());
            assertEquals(multi, restored.isMulti());
            assertEquals(json, restored.toJson());
        }
    }

    @Test
    public void testToJsonWithoutValueTypeShouldKeepMulti() {
        EnumType type = new EnumType();
        type.setMulti(true);

        JSONObject json = type.toJson();

        Assert.assertFalse(json.containsKey("elements"));
        Assert.assertFalse(json.containsKey("valueType"));
        assertEquals(Boolean.TRUE, json.getBoolean("multi"));

        EnumType restored = new EnumType();
        restored.fromJson(json);
        Assert.assertNull(restored.getElements());
        Assert.assertNull(restored.getValueType());
        assertTrue(restored.isMulti());
    }

    @Test
    public void testToJsonWithEmptyElementsShouldKeepEmptyList() {
        EnumType type = new EnumType();
        type.setElements(Collections.emptyList());
        type.setValueType(IntType.GLOBAL);
        type.setMulti(true);

        JSONObject json = type.toJson();

        assertNotNull(json.getJSONArray("elements"));
        assertTrue(json
            .getJSONArray("elements")
            .isEmpty());
        assertEquals(IntType.ID, json
            .getJSONObject("valueType")
            .getString("type"));
        assertEquals(Boolean.TRUE, json.getBoolean("multi"));

        EnumType restored = new EnumType();
        restored.fromJson(json);
        assertNotNull(restored.getElements());
        assertTrue(restored
            .getElements()
            .isEmpty());
        assertEquals(json, restored.toJson());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testSafeSerializationShouldKeepNestedEnumConfiguration() {
        for (boolean multi : new boolean[]{false, true}) {
            EnumType type = new EnumType();
            type.setValueType(StringType.GLOBAL);
            type.setMulti(multi);
            DefaultConfigMetadata metadata = new DefaultConfigMetadata()
                .add("mediaMode", "素材模式", type);

            JSONObject serialized = new JSONObject((Map<String, Object>) SerializeUtils
                .convertToSafelySerializable(metadata));
            JSONObject typeJson = serialized
                .getJSONArray("properties")
                .getJSONObject(0)
                .getJSONObject("type");

            Assert.assertFalse(typeJson.containsKey("elements"));
            assertNotNull(typeJson.getJSONObject("valueType"));
            assertEquals(StringType.ID, typeJson
                .getJSONObject("valueType")
                .getString("type"));
            assertEquals(Boolean.valueOf(multi), typeJson.getBoolean("multi"));

            EnumType restored = new EnumType();
            restored.fromJson(typeJson);
            assertNotNull(restored.getValueType());
            assertEquals(StringType.ID, restored
                .getValueType()
                .getId());
            assertEquals(multi, restored.isMulti());
        }
    }

}