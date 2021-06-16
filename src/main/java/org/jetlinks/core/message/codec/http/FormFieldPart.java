package org.jetlinks.core.message.codec.http;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.springframework.http.HttpHeaders;

public interface FormFieldPart extends Part {

    String getValue();

    static FormFieldPart of(String name, String value, HttpHeaders headers) {
        return of(name, value, headers, Unpooled.EMPTY_BUFFER);
    }

    static FormFieldPart of(String name, String value, HttpHeaders headers, ByteBuf byteBuf) {
        return new SimpleFormFieldPart(name, value, headers, byteBuf);
    }
}
