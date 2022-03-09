package org.jetlinks.core.route;

import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

public interface HttpRoute extends Route {

    HttpMethod[] getMethod();

    MediaType[] getContentType();

    static Builder builder() {
        return DefaultHttpRoute.builder();
    }

    interface Builder {
        Builder group(String group);

        Builder method(HttpMethod... method);

        Builder contentType(MediaType... contentType);

        Builder address(String address);

        Builder description(String description);

        Builder example(String example);

        HttpRoute build();
    }
}
