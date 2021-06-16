package org.jetlinks.core.message.codec.http;

import io.netty.buffer.ByteBuf;
import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpHeaders;

@Getter
@Setter
class SimpleFormFieldPart extends SimplePart implements FormFieldPart {

    private final String value;

    public SimpleFormFieldPart(String name, String value, HttpHeaders headers, ByteBuf content) {
        super(name, headers, content);
        this.value = value;
    }

    @Override
    public String toString() {
        return "FormFieldPart{" +
                "name='" + getName() + '\'' +
                ",value='" + getValue() + '\'' +
                '}';
    }

}
