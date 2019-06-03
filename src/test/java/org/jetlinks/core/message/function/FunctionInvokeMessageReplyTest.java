package org.jetlinks.core.message.function;

import org.jetlinks.core.enums.ErrorCode;
import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.*;

public class FunctionInvokeMessageReplyTest {

    @Test
    public void test() {
        FunctionInvokeMessageReply reply = FunctionInvokeMessageReply.create();

        reply.error(ErrorCode.TIME_OUT).messageId("test");

        Assert.assertFalse(reply.isSuccess());
        Assert.assertEquals(reply.getCode(), ErrorCode.TIME_OUT.name());
        Assert.assertEquals(reply.getMessage(), ErrorCode.TIME_OUT.getText());
        Assert.assertEquals(reply.getMessageId(), "test");

        reply.success(1);

        Assert.assertTrue(reply.isSuccess());
        Assert.assertEquals(reply.getOutput(),1);

    }
}