package org.jetlinks.core.message.codec;

import org.eclipse.californium.core.coap.CoAP;
import org.junit.Test;

import static org.junit.Assert.*;

public class DefaultCoapResponseMessageTest {


    @Test
    public void test() {
        DefaultCoapResponseMessage responseMessage = DefaultCoapResponseMessage.of(
                String.join("\n"
                        , "CREATED 2.01"
                        , ""
                        , "ok"
                )
        );
        System.out.println(responseMessage.print(true));
        assertEquals(responseMessage.getCode(), CoAP.ResponseCode.CREATED);
        assertEquals(responseMessage.payloadAsString(), "ok");
    }

    @Test
    public void testText() {
        DefaultCoapResponseMessage responseMessage = DefaultCoapResponseMessage.of(
                String.join("\n"
                        , "CREATED"
                        , "1022: 1"
                        , ""
                        , "ok"
                )
        );
        System.out.println(responseMessage.print(true));
        assertEquals(responseMessage.getCode(), CoAP.ResponseCode.CREATED);
        assertEquals(responseMessage.payloadAsString(), "ok");
    }

    @Test
    public void testCode() {
        DefaultCoapResponseMessage responseMessage = DefaultCoapResponseMessage.of(
                String.join("\n"
                        , "2.01"
                        , "1022: 1"
                        , "Content-Format: application/json"
                        , ""
                        , "ok"
                )
        );
        System.out.println(responseMessage.print(true));
        assertEquals(responseMessage.getCode(), CoAP.ResponseCode.CREATED);
        assertEquals(responseMessage.payloadAsString(), "ok");
    }

    @Test
    public void testPrint() {
        DefaultCoapResponseMessage responseMessage = DefaultCoapResponseMessage.of(
                String.join("\n"
                        , "CREATED 2.01\n" +
                                    "Content-Format: application/json\n" +
                                    "\n" +
                                    "{\"success\":true}"
                )
        );
        System.out.println(responseMessage.print(true));
        assertEquals(responseMessage.getCode(), CoAP.ResponseCode.CREATED);
        assertEquals(responseMessage.payloadAsString(), "{\"success\":true}");
    }

}