package org.jetlinks.core.message.codec.http;

import org.junit.Test;
import org.springframework.http.MediaType;

import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;

public class SimpleHttpResponseMessageTest {

    @Test
    public void testJson(){

        String http =String.join("\n",
                "HTTP 200 OK"
                ,"Content-Type: application/json"
                ,""
                ,"{\"success\":\"true\"}"
        );

        SimpleHttpResponseMessage message=SimpleHttpResponseMessage.of(http);
        System.out.println( message.print());
        assertEquals(message.getStatus(),200);
        assertEquals(message.getContentType(), MediaType.APPLICATION_JSON);

        assertEquals(message.getPayload().toString(StandardCharsets.UTF_8),"{\"success\":\"true\"}");

    }


}