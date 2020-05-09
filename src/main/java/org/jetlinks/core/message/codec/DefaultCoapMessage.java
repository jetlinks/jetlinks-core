package org.jetlinks.core.message.codec;

import io.netty.buffer.ByteBuf;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.Option;

import javax.annotation.Nonnull;
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

}
