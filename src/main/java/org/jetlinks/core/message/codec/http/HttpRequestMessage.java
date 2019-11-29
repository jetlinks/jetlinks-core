package org.jetlinks.core.message.codec.http;

import org.jetlinks.core.message.codec.EncodedMessage;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface HttpRequestMessage extends EncodedMessage {

    @Nonnull
    String getUrl();

    @Nonnull
    HttpMethod getMethod();

    @Nullable
    MediaType getContentType();

    @Nonnull
    List<Header> getHeaders();

    @Nullable
    Map<String, String> getQueryParameters();

    /**
     * @see org.springframework.http.MediaType#APPLICATION_FORM_URLENCODED_VALUE 专属
     */
    @Nullable
    Map<String, String> getRequestParam();

    default Optional<Header> getHeader(String name) {
        return getHeaders().stream()
                .filter(header -> header.getName().equals(name))
                .findFirst();
    }

    default Optional<String> getQueryParameter(String name) {
        return Optional.ofNullable(getQueryParameters())
                .map(map -> map.get(name));
    }


}
