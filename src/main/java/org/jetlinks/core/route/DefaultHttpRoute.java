package org.jetlinks.core.route;

import lombok.Getter;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

@Getter
class DefaultHttpRoute implements HttpRoute {
    private final String group;
    private final HttpMethod[] method;
    private final MediaType[] contentType;
    private final String address;
    private final String description;
    private final String example;

    DefaultHttpRoute(String group,
                     HttpMethod[] method,
                     MediaType[] contentType,
                     String address,
                     String description,
                     String example) {
        this.group = group;
        this.method = method;
        this.contentType = contentType;
        this.address = address;
        this.description = description;
        this.example = example;
    }

    static DefaultHttpRouteBuilder builder() {
        return new DefaultHttpRouteBuilder();
    }

    static class DefaultHttpRouteBuilder implements Builder {
        private String group;
        private HttpMethod[] method;
        private MediaType[] contentType;
        private String address;
        private String description;
        private String example;

        DefaultHttpRouteBuilder() {
        }

        public DefaultHttpRouteBuilder group(String group) {
            this.group = group;
            return this;
        }

        public DefaultHttpRouteBuilder method(HttpMethod... method) {
            this.method = method;
            return this;
        }

        public DefaultHttpRouteBuilder contentType(MediaType... contentType) {
            this.contentType = contentType;
            return this;
        }

        public DefaultHttpRouteBuilder address(String address) {
            this.address = address;
            return this;
        }

        public DefaultHttpRouteBuilder description(String description) {
            this.description = description;
            return this;
        }

        public DefaultHttpRouteBuilder example(String example) {
            this.example = example;
            return this;
        }

        public DefaultHttpRoute build() {
            return new DefaultHttpRoute(group, method, contentType, address, description, example);
        }

        public String toString() {
            return "DefaultHttpRoute.DefaultHttpRouteBuilder(group=" + this.group + ", method=" + java.util.Arrays.deepToString(this.method) + ", contentType=" + java.util.Arrays.deepToString(this.contentType) + ", address=" + this.address + ", description=" + this.description + ", example=" + this.example + ")";
        }
    }
}
