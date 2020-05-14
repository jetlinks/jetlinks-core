package org.jetlinks.core.message.codec;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.Getter;
import lombok.Setter;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.Option;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class DefaultCoapResponseMessage implements CoapResponseMessage {

    @Nonnull
    private CoAP.ResponseCode code;

    private List<Option> options;

    private ByteBuf payload;

    /**
     * CoAP 5.02
     *
     * @param text
     * @return
     */
    public static DefaultCoapResponseMessage of(String text) {
        DefaultCoapResponseMessage msg = new DefaultCoapResponseMessage();
        List<Option> options = new ArrayList<>();
        msg.setOptions(options);
        TextMessageParser.of(
                (start) -> {
                    String[] arr = start.split("[ ]");
                    String str = arr.length == 1 ? arr[0] : arr[1];
                    if (str.contains(".")) {
                        String[] codeArr = str.split("[.]");
                        msg.setCode(CoAP.ResponseCode.valueOf(Integer.parseInt(codeArr[0]) << 5 | Integer.parseInt(codeArr[1])));
                    } else {
                        msg.setCode(CoAP.ResponseCode.valueOf(str));
                    }
                },
                (option, value) -> {
                    options.add(CoapMessage.parseOption(option, value));
                },
                body -> {
                    msg.setPayload(Unpooled.wrappedBuffer(body.getBody()));
                },
                () -> {
                    msg.setPayload(Unpooled.wrappedBuffer(new byte[0]));
                }
        ).parse(text);

        return msg;
    }

    public static DefaultCoapResponseMessage of(CoapResponse response) {
        DefaultCoapResponseMessage msg = new DefaultCoapResponseMessage();
        msg.setCode(response.getCode());
        msg.setOptions(response.getOptions().asSortedList());
        msg.setPayload(Unpooled.wrappedBuffer(response.getPayload() == null ? new byte[0] : response.getPayload()));
        return msg;
    }
}
