package org.jetlinks.core.message.codec.http;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.http.MediaType;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SimpleHttpResponseMessage implements HttpResponseMessage {

    private int status;

    private MediaType contentType;

    private List<Header> headers = new ArrayList<>();

    private ByteBuf payload;

    public static SimpleHttpResponseMessageBuilder builder() {
        return new SimpleHttpResponseMessageBuilder();
    }

    @Nonnull
    @Override
    public List<Header> getHeaders() {
        return headers == null ? Collections.emptyList() : headers;
    }

    public static class SimpleHttpResponseMessageBuilder {
        private int status;
        private MediaType contentType;
        private List<Header> headers;
        private ByteBuf payload;

        SimpleHttpResponseMessageBuilder() {
        }

        public SimpleHttpResponseMessageBuilder body(String payload) {
            return payload(payload.getBytes());
        }

        public SimpleHttpResponseMessageBuilder body(byte[] payload) {
            return payload(Unpooled.wrappedBuffer(payload));
        }

        public SimpleHttpResponseMessageBuilder payload(String payload) {
            return payload(payload.getBytes());
        }

        public SimpleHttpResponseMessageBuilder payload(byte[] payload) {
            return payload(Unpooled.wrappedBuffer(payload));
        }

        public SimpleHttpResponseMessageBuilder contentType(String mediaType) {
            return contentType(MediaType.valueOf(mediaType));
        }

        public SimpleHttpResponseMessageBuilder header(String key, String... values) {
            if (headers == null) {
                headers = new ArrayList<>();
            }
            headers.add(new Header(key, values));
            return this;
        }

        public SimpleHttpResponseMessageBuilder headers(Map<String, Object> headers) {
            headers.forEach((k, v) -> header(k, String.valueOf(v)));
            return this;
        }

        public SimpleHttpResponseMessageBuilder status(int status) {
            this.status = status;
            return this;
        }

        public SimpleHttpResponseMessageBuilder contentType(MediaType contentType) {
            this.contentType = contentType;
            return this;
        }

        public SimpleHttpResponseMessageBuilder headers(List<Header> headers) {
            this.headers = headers;
            return this;
        }

        public SimpleHttpResponseMessageBuilder payload(ByteBuf payload) {
            this.payload = payload;
            return this;
        }

        public SimpleHttpResponseMessage build() {
            return new SimpleHttpResponseMessage(status, contentType, headers, payload);
        }

        public String toString() {
            return "SimpleHttpResponseMessage.SimpleHttpResponseMessageBuilder(status=" + this.status + ", contentType=" + this.contentType + ", headers=" + this.headers + ", payload=" + this.payload + ")";
        }
    }
}
