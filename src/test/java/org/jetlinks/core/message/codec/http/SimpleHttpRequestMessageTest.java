package org.jetlinks.core.message.codec.http;

import org.junit.Test;
import org.springframework.http.MediaType;

import java.nio.charset.StandardCharsets;
import java.util.Collections;

import static org.junit.Assert.*;

public class SimpleHttpRequestMessageTest {

    @Test
    public void testJson(){

        String http =String.join("\n",
                "POST http://www.baidu.com/?q=jetlinks"
                ,"Content-Type: application/json"
                ,""
                ,"{\"hello\":\"world\"}"
                );

        SimpleHttpRequestMessage message=SimpleHttpRequestMessage.of(http);
        System.out.println( message.print());
        assertEquals(message.getUrl(),"http://www.baidu.com/");
        assertEquals(message.getQueryParameters(), Collections.singletonMap("q","jetlinks"));

        assertEquals(message.getContentType(), MediaType.APPLICATION_JSON);

        assertEquals(message.getPayload().toString(StandardCharsets.UTF_8),"{\"hello\":\"world\"}");

    }

    @Test
    public void testFormData(){

        String http =String.join("\n",
                "POST http://www.baidu.com/?q=jetlinks"
                ,""
                ,"b=c&d=e"
        );

        SimpleHttpRequestMessage message=SimpleHttpRequestMessage.of(http);
        System.out.println( message.print());
        assertEquals(message.getUrl(),"http://www.baidu.com/");
        assertEquals(message.getQueryParameters(), Collections.singletonMap("q","jetlinks"));

        assertEquals(message.getContentType(), MediaType.APPLICATION_FORM_URLENCODED);

        assertEquals(message.getPayload().toString(StandardCharsets.UTF_8),"b=c&d=e");
        assertEquals(message.getRequestParam().get("b"),"c");
        assertEquals(message.getRequestParam().get("d"),"e");

    }

}