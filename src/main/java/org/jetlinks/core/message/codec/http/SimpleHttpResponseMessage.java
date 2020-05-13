package org.jetlinks.core.message.codec.http;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;

import javax.annotation.Nonnull;
import java.util.*;

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

    /**
     * <pre>
     *     HTTP/1.1 200 OK
     *     Content-Type: application/json
     *
     *     {"success":true}
     * </pre>
     * @param httpString httpString
     * @return
     */
    @SuppressWarnings("all")
    public static SimpleHttpResponseMessage of(String httpString){
        SimpleHttpResponseMessage response=new SimpleHttpResponseMessage();
        String[] lines = httpString.split("[\n]");
        String[] firstLine = lines[0].split("[ ]");

        response.setStatus(Integer.parseInt(firstLine[1].trim()));

        HttpHeaders httpHeaders = new HttpHeaders();
        int lineIndex = 1;
        for (; lineIndex < lines.length; lineIndex++) {
            String[] line = lines[lineIndex].split("[:]");
            if (!StringUtils.isEmpty(line[0])) {
                if (line.length > 1) {
                    httpHeaders.add(line[0].trim(), line[1].trim());
                }
            } else {
                break;
            }
        }
        response.setContentType(httpHeaders.getContentType());

        String body = null;
        //body
        if (lineIndex < lines.length) {
            body = String.join("\n", Arrays.copyOfRange(lines, lineIndex, lines.length)).trim();
            //识别contentType
            if (response.getContentType() == null) {
                if (body.startsWith("[") || body.startsWith("{")) {
                    response.setContentType(MediaType.APPLICATION_JSON);
                } else {
                    response.setContentType(MediaType.TEXT_PLAIN);
                }
            }
            response.setPayload(Unpooled.wrappedBuffer(body.getBytes()));
        } else {
            response.setPayload(Unpooled.wrappedBuffer(new byte[0]));
        }

        return response;
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
            if (payload == null) {
                payload = Unpooled.wrappedBuffer(new byte[0]);
            }
            return new SimpleHttpResponseMessage(status, contentType, headers, payload);
        }

        public String toString() {
            return "SimpleHttpResponseMessage.SimpleHttpResponseMessageBuilder(status=" + this.status + ", contentType=" + this.contentType + ", headers=" + this.headers + ", payload=" + this.payload + ")";
        }
    }
}
