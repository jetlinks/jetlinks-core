package org.jetlinks.core.message;

import com.alibaba.fastjson.JSON;
import org.jetlinks.core.message.function.FunctionInvokeMessage;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class MessageTypeTest {


    @Test
    public void testUnknown() {
        Map<String, Object> val = new HashMap<>();
        val.put("messageId", "test");

        Message message = MessageType.convertMessage(val).orElseThrow(IllegalArgumentException::new);

        assertTrue(message instanceof CommonDeviceMessage);

        assertEquals(message.getMessageId(), val.get("messageId"));
    }

    @Test
    public void testUnknownReply() {
        Map<String, Object> val = new HashMap<>();
        val.put("messageId", "test");
        val.put("success", "true");

        Message message = MessageType.convertMessage(val).orElseThrow(IllegalArgumentException::new);

        assertTrue(message instanceof CommonDeviceMessageReply);

        assertEquals(message.getMessageId(), val.get("messageId"));
        assertTrue(((CommonDeviceMessageReply<?>) message).isSuccess());
    }

    @Test
    public void testDirect() {
        DirectDeviceMessage msg = new DirectDeviceMessage();
        msg.setPayload("hello".getBytes());

        Message message = MessageType
                .convertMessage(JSON.parseObject(msg.toString()))
                .orElseThrow(IllegalArgumentException::new);

        assertTrue(message instanceof DirectDeviceMessage);
        assertArrayEquals(((DirectDeviceMessage) message).getPayload(), msg.getPayload());

    }

    @Test
    public void testFunctionInvokeMapInputs() {
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("name", "name1");
        inputs.put("value", 1);

        FunctionInvokeMessage message = MessageType.INVOKE_FUNCTION.convert(Collections.singletonMap("inputs", inputs));

        assertEquals(message.inputsToMap(),inputs);

    }
}