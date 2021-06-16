package org.jetlinks.core.message.codec.http;

import io.netty.buffer.ByteBuf;
import org.springframework.http.HttpHeaders;

public interface Part {

    String getName();

    HttpHeaders getHeaders();

    ByteBuf getContent();


}
