package org.jetlinks.core.message.codec;

import java.util.List;
import java.util.Optional;

public interface HttpMessage extends EncodedMessage {

    List<Header> getHeaders();

    Optional<String> getHeader(String name);

    Optional<String> getQueryParameter(String name);

    class Header {
        private String name;

        private String[] value;
    }
}
