package org.jetlinks.core.message.function;

import com.alibaba.fastjson.JSONObject;
import org.junit.Assert;
import org.junit.Test;

public class FunctionInvokeMessageTest {

    @Test
    public void test() {
        FunctionInvokeMessage message = new FunctionInvokeMessage();

        Assert.assertNotNull(message.getInputs());

        message.addInput("test",1);

        Assert.assertEquals(message.getInputs().size(),1);

        Assert.assertEquals(message.getInputs().get(0).getName(),"test");
        Assert.assertEquals(message.getInputs().get(0).getValue(),1);

        message.fromJson(new JSONObject(){
            {
                put("code","1");
                put("messageId","M1");
            }
        });

        Assert.assertEquals(message.getCode(),"1");
        Assert.assertEquals(message.getMessageId(),"M1");




    }

}