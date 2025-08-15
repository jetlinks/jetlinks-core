package org.jetlinks.core.metadata.types;


import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import org.jetlinks.core.metadata.Jsonable;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;


/**
 * 抽象测试基类
 */
public abstract class JsonableTestBase<T extends Jsonable> {

    protected abstract T newInstance();

    protected abstract void fillSampleData(T instance);

    protected abstract void assertSampleData(T instance);

    @Test
    public void testToJsonAndFromJson() {
        T original = newInstance();
        fillSampleData(original);

        JSONObject json = original.toJson();
        System.out.println("======toJson======");
        System.out.println(JSONObject.toJSONString(json, SerializerFeature.PrettyFormat));
        assertNotNull(json);

        T copy = newInstance();
        copy.fromJson(json);
        assertSampleData(copy);

        JSONObject json2 = copy.toJson();
        System.out.println("======fromJson then toJson======");
        System.out.println(JSONObject.toJSONString(json2, SerializerFeature.PrettyFormat));
        assertEquals(json, json2);
    }

    public abstract static class Empty<C extends Jsonable> extends JsonableTestBase<C> {

        @Override
        protected void fillSampleData(C instance) {
        }

        @Override
        protected void assertSampleData(C instance) {
        }
    }
}