package org.jetlinks.core.message.codec.http;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import org.jetlinks.core.message.codec.MessagePayloadType;
import org.jetlinks.core.message.codec.TextMessageParser;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Getter
@Setter
public class SimpleHttpRequestMessage implements HttpRequestMessage {

    //消息体
    private ByteBuf payload;

    private String path;

    private String url;

    //请求方法
    private HttpMethod method;

    //请求头
    private List<Header> headers;

    //参数
    private Map<String, String> queryParameters;

    //请求类型
    private MediaType contentType;

    @SneakyThrows
    @SuppressWarnings("all")
    public static SimpleHttpRequestMessage of(String httpString) {
        SimpleHttpRequestMessage request = new SimpleHttpRequestMessage();
        HttpHeaders httpHeaders = new HttpHeaders();
        TextMessageParser.of(
                start -> {
                    String[] firstLine = start.split("[ ]");
                    String method = firstLine[0];
                    String url = firstLine[1];
                    if (url.contains("?")) {
                        String parameters = url.substring(url.indexOf("?") + 1);
                        url = url.substring(0, url.indexOf("?"));
                        request.setQueryParameters(
                                Stream.of(parameters.split("[&]"))
                                        .map(str -> str.split("[=]", 2))
                                        .filter(arr -> arr.length > 1)
                                        .collect(Collectors.toMap(arr -> arr[0], arr -> arr[1], (a, b) -> String.join(",", a, b)))
                        );
                    }
                    request.setMethod(HttpMethod.resolve(method));
                    request.setPath(HttpUtils.getUrlPath(url));
                    request.setUrl(url);
                },
                httpHeaders::add,
                body -> {
                    request.setPayload(Unpooled.wrappedBuffer(body.getBody()));

                    if (httpHeaders.getContentType() == null) {
                        if (body.getType() == MessagePayloadType.JSON) {
                            httpHeaders.setContentType(MediaType.APPLICATION_JSON);
                        } else if (body.getType() == MessagePayloadType.STRING) {
                            httpHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
                        }
                    }
                    request.setContentType(httpHeaders.getContentType());
                },
                () -> {
                    if (request.getContentType() == null) {
                        request.setContentType(httpHeaders.getContentType());
                    }
                    request.setPayload(Unpooled.EMPTY_BUFFER);
                }

        ).parse(httpString);

        request.setHeaders(httpHeaders.entrySet()
                .stream()
                .map(e -> new Header(e.getKey(), e.getValue().toArray(new String[0])))
                .collect(Collectors.toList()));

        return request;
    }
}
