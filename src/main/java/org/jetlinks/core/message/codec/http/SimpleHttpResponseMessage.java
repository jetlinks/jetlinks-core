package org.jetlinks.core.message.codec.http;

import io.netty.buffer.ByteBuf;
import lombok.*;
import org.springframework.http.MediaType;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SimpleHttpResponseMessage implements HttpResponseMessage {

    private int status;

    private MediaType contentType;

    private List<Header> headers = new ArrayList<>();

    private ByteBuf payload;

    @Nonnull
    @Override
    public List<Header> getHeaders() {
        return headers == null ? Collections.emptyList() : headers;
    }

}
