package org.jetlinks.core.message.property;

import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.*;

public class ReadPropertyMessageTest {

    @Test
    public void test(){
        ReadPropertyMessage message=new ReadPropertyMessage();
        message.setMessageId("test");

        ReadPropertyMessageReply reply=message.newReply();
        Assert.assertNotNull(reply);
        Assert.assertEquals(reply.getMessageId(),"test");

    }
}