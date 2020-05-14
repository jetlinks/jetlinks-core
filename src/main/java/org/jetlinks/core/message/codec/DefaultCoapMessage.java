package org.jetlinks.core.message.codec;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.*;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.Option;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DefaultCoapMessage implements CoapMessage {

    @Nonnull
    private String path;

    private CoAP.Code code;

    @Nonnull
    private ByteBuf payload;

    @Getter
    private List<Option> options;

    @Override
    public String toString() {
        return print(true);
    }


    @SneakyThrows
    public static DefaultCoapMessage of(String coapString) {
        DefaultCoapMessage request = new DefaultCoapMessage();
        List<Option> options = new ArrayList<>();
        request.setOptions(options);
        TextMessageParser.of(
                start -> {
                    String[] firstLine = start.split("[ ]");
                    request.setCode(CoAP.Code.valueOf(firstLine[0]));
                    request.setPath(firstLine[1]);
                },
                (option, value) -> options.add(CoapMessage.parseOption(option,value)),
                body -> request.setPayload(Unpooled.wrappedBuffer(body.getBody())),
                () -> request.setPayload(Unpooled.wrappedBuffer(new byte[0]))
        ).parse(coapString);

        return request;

    }

}
