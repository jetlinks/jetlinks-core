package org.jetlinks.core.message.codec.http;

import org.jetlinks.core.message.codec.EncodedMessage;
import org.springframework.http.MediaType;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Optional;

public interface HttpResponseMessage extends EncodedMessage {

    int getStatus();

    MediaType getContentType();

    @Nonnull
    List<Header> getHeaders();

    default Optional<Header> getHeader(String name) {
        return getHeaders().stream()
                .filter(header -> header.getName().equals(name))
                .findFirst();
    }

}
