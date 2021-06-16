package org.jetlinks.core.message.codec.http;

import io.netty.buffer.ByteBuf;
import lombok.Getter;
import org.springframework.http.HttpHeaders;

@Getter
public class SimpleFilePart extends SimplePart implements FilePart {
    private final String fileName;

    public SimpleFilePart(String name, String fileName, HttpHeaders headers, ByteBuf content) {
        super(name, headers, content);
        this.fileName = fileName;
    }

    @Override
    public String toString() {
        return "FilePart{" +
                "name='" + getName() + '\'' +
                ",fileName='" + fileName + '\'' +
                ",fileSize=" + getContent().readableBytes() +
                '}';
    }
}
