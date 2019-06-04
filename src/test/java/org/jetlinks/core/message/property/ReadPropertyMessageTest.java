package org.jetlinks.core.message.property;

import org.junit.Assert;
import org.junit.Test;

public class ReadPropertyMessageTest {

    @Test
    public void test(){
        ReadPropertyMessage message=new ReadPropertyMessage();
        message.setMessageId("test");
        message.addHeader("test","test");

        Assert.assertTrue(message.getHeader("test").isPresent());

        ReadPropertyMessageReply reply=message.newReply();
        Assert.assertNotNull(reply);
        Assert.assertEquals(reply.getMessageId(),"test");

    }
}