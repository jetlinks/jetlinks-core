package org.jetlinks.core.message.codec.http;

import io.netty.buffer.ByteBuf;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpHeaders;

@AllArgsConstructor
@Getter
class SimplePart implements Part {
    private final String name;
    private final HttpHeaders headers;
    private final ByteBuf content;

    @Override
    public String toString() {
        return "Part{" +
                "name='" + name + '\'' +
                '}';
    }
}
