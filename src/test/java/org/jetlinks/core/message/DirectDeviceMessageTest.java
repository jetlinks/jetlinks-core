package org.jetlinks.core.message;

import com.alibaba.fastjson.JSON;
import org.junit.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.Assert.*;

public class DirectDeviceMessageTest {

    @Test
    public void testFromJson(){
        DirectDeviceMessage message=new DirectDeviceMessage();
        message.setDeviceId("test");
        message.setPayload("test".getBytes(StandardCharsets.UTF_8));

        String json = message.toJson().toJSONString();

        DirectDeviceMessage target=new DirectDeviceMessage();
        target.fromJson(JSON.parseObject(json));

        assertEquals(target.getDeviceId(),message.getDeviceId());
        assertArrayEquals(target.getPayload(),message.getPayload());

    }
}