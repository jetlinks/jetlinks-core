package org.jetlinks.core.message.codec.http;

import io.netty.buffer.ByteBuf;
import org.springframework.http.HttpHeaders;

public interface FilePart extends Part{

    String getFileName();

    static FilePart of(String name, String fileName, HttpHeaders headers, ByteBuf fileBody) {
        return new SimpleFilePart(name, fileName, headers, fileBody);
    }
}
