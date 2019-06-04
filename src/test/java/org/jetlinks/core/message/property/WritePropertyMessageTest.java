package org.jetlinks.core.message.property;

import org.junit.Assert;
import org.junit.Test;

public class WritePropertyMessageTest {

    @Test
    public void test() {
        WritePropertyMessage message = new WritePropertyMessage();
        message.addHeader("test","test");

        Assert.assertTrue(message.getHeader("test").isPresent());

        message.setMessageId("test");

        WritePropertyMessageReply reply = message.newReply();
        Assert.assertNotNull(reply);

        Assert.assertEquals(reply.getMessageId(), message.getMessageId());
    }

}