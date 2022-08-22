package org.jetlinks.core.message.property;

import com.alibaba.fastjson.JSONObject;
import org.jetlinks.core.enums.ErrorCode;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;

public class ReadPropertyMessageReplyTest {


    @Test
    public void testJson(){
        ReadPropertyMessageReply reply=new ReadPropertyMessageReply();

        JSONObject object= JSONObject.parseObject("{\"propertySourceTimes\":{\"test\":123456}}");

        reply.fromJson(object);

        Assert.assertEquals(reply.getPropertySourceTimes().get("test"),Long.valueOf(123456L));

    }
    @Test
    public void test() {
        ReadPropertyMessageReply reply = ReadPropertyMessageReply.create();

        reply.error(ErrorCode.TIME_OUT);
        reply.addHeader("test","test");

        Assert.assertTrue(reply.getHeader("test").isPresent());

        Assert.assertFalse(reply.isSuccess());
        Assert.assertEquals(reply.getCode(), ErrorCode.TIME_OUT.name());
        Assert.assertEquals(reply.getMessage(), ErrorCode.TIME_OUT.getText());

        Assert.assertTrue(reply.getTimestamp() != 0);

        reply.success(Collections.singletonMap("test","1"));

        Assert.assertTrue(reply.isSuccess());
        Assert.assertNotNull(reply.getProperties());
        Assert.assertEquals(reply.getProperties().get("test"),"1");


    }

}