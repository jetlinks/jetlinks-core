package org.jetlinks.core.message.codec;

import org.apache.commons.codec.binary.Hex;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.eclipse.californium.core.coap.Option;
import org.eclipse.californium.core.coap.OptionNumberRegistry;
import org.junit.Test;

import java.util.Objects;

import static org.junit.Assert.assertEquals;

public class DefaultCoapMessageTest {

    @Test
    public void test() {

        String str = String.join("\n",
                "POST /test"
                , "Content-Format: application/json"
                , ""
                , "hello"
                , "world"
        );
        DefaultCoapMessage message = DefaultCoapMessage.of(str);
        System.out.println(message.print(true));
        assertEquals(message.getCode(), CoAP.Code.POST);

        assertEquals(message.getOption(OptionNumberRegistry.CONTENT_FORMAT)
                .map(opt -> MediaTypeRegistry.toString(opt.getIntegerValue()))
                .orElse(null), "application/json");

        assertEquals(message.payloadAsString(), "hello\nworld");

    }

    @Test
    public void testHex() {

        String str = String.join("\n",
                "POST /test"
                , "1012: 0x02"
                , ""
                , "0x001a1231"
        );
        DefaultCoapMessage message = DefaultCoapMessage.of(str);
        System.out.println(message.print(false));
        assertEquals(message.getCode(), CoAP.Code.POST);

        assertEquals(Objects.requireNonNull(message.getOption(1012)
                .map(Option::getIntegerValue)
                .orElse(null)).intValue(), 2);

        assertEquals(Hex.encodeHexString(message.payloadAsBytes()), "001a1231");

    }

}