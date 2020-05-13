package org.jetlinks.core.message.codec.http;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;

import java.net.URLDecoder;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Getter
@Setter
public class SimpleHttpRequestMessage implements HttpRequestMessage {

    //消息体
    private ByteBuf payload;

    private String url;

    //请求方法
    private HttpMethod method;

    //请求头
    private List<Header> headers;

    //参数
    private Map<String, String> queryParameters;

    private Map<String, String> requestParam;

    //请求类型
    private MediaType contentType;

    @SneakyThrows
    @SuppressWarnings("all")
    public static SimpleHttpRequestMessage of(String httpString) {
        SimpleHttpRequestMessage request = new SimpleHttpRequestMessage();
        String[] lines = httpString.split("[\n]");
        String[] firstLine = lines[0].split("[ ]");
        String method = firstLine[0];
        String url = firstLine[1];

        // POST http://www.baidu.com?s=jetlinks
        if (url.contains("?")) {
            String parameters = URLDecoder.decode(url.substring(url.indexOf("?") + 1), "UTF-8");
            url = url.substring(0, url.indexOf("?"));
            request.setQueryParameters(
                    Stream.of(parameters.split("[&]"))
                            .map(str -> str.split("[=]"))
                            .filter(arr -> arr.length > 1)
                            .collect(Collectors.toMap(arr -> arr[0], arr -> arr[1]))
            );
        }
        request.setMethod(HttpMethod.resolve(method));
        request.setUrl(url);

        //headers
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
        request.setHeaders(httpHeaders
                .entrySet()
                .stream()
                .map(e -> new Header(e.getKey(), e.getValue().toArray(new String[0])))
                .collect(Collectors.toList()));

        request.setContentType(httpHeaders.getContentType());

        String body = null;
        //body
        if (lineIndex < lines.length) {
            body = String.join("\n", Arrays.copyOfRange(lines, lineIndex, lines.length)).trim();
            //识别contentType
            if (request.getContentType() == null) {
                if (body.startsWith("[") || body.startsWith("{")) {
                    request.setContentType(MediaType.APPLICATION_JSON);
                } else {
                    request.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
                }
            }
            request.setPayload(Unpooled.wrappedBuffer(body.getBytes()));
        } else {
            request.setPayload(Unpooled.wrappedBuffer(new byte[0]));
        }

        if (body != null && MediaType.APPLICATION_FORM_URLENCODED.includes(request.getContentType())) {
            request.setRequestParam(
                    Stream.of(body.split("[&]"))
                            .map(str -> str.split("[=]"))
                            .filter(arr -> arr.length > 1)
                            .collect(Collectors.toMap(arr -> arr[0], arr -> arr[1]))
            );
        }
        return request;
    }
}
