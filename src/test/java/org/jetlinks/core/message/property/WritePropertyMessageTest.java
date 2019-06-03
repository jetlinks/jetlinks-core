package org.jetlinks.core.message.property;

import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.*;

public class WritePropertyMessageTest {

    @Test
    public void test() {
        WritePropertyMessage message = new WritePropertyMessage();


        message.setMessageId("test");

        WritePropertyMessageReply reply = message.newReply();
        Assert.assertNotNull(reply);

        Assert.assertEquals(reply.getMessageId(), message.getMessageId());
    }

}