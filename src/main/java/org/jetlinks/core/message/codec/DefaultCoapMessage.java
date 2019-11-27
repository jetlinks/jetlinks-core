package org.jetlinks.core.message.codec;

import io.netty.buffer.ByteBuf;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.eclipse.californium.core.coap.Option;

import javax.annotation.Nonnull;
import java.util.List;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DefaultCoapMessage implements CoapMessage {

    @Getter
    @Nonnull
    private String path;

    @Getter
    @Nonnull
    private ByteBuf payload;

    @Getter
    private List<Option> options;


}
