package org.jetlinks.core.things.relation;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import org.junit.Assert;
import org.junit.Test;

public class ObjectSpecTest {

    @Test
    public void test() {

        ObjectSpec spec = ObjectSpec.parse("dev1@device:manager@user:member@user");
        Assert.assertNotNull(spec);

        System.out.println(spec);

        System.out.println(JSON.toJSONString(spec, SerializerFeature.PrettyFormat));
    }

    @Test
    public void tesOpt() {

        ObjectSpec spec = ObjectSpec.parse("dev1@device:manager$reverse@user:member@user");
        Assert.assertNotNull(spec);

        System.out.println(spec);

        System.out.println(JSON.toJSONString(spec, SerializerFeature.PrettyFormat));
    }
}