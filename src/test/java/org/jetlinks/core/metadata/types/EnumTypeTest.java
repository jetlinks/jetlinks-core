package org.jetlinks.core.metadata.types;

import com.google.common.collect.Lists;
import org.junit.Assert;
import org.junit.Test;

import static org.hibernate.validator.internal.util.Contracts.assertNotNull;
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

}