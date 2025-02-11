package org.jetlinks.core.message.codec.http;

import com.alibaba.fastjson.JSON;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import org.jetlinks.core.message.codec.EncodedMessage;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.util.CollectionUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public interface HttpRequestMessage extends EncodedMessage {

    /**
     * 获取请求相对地址，不包含url参数。
     *
     * @return 请求地址
     */
    @Nonnull
    default String getPath() {
        return HttpUtils.getUrlPath(getUrl());
    }

    /**
     * 获取请求相对地址，包含url参数。
     *
     * @return 请求地址
     */
    @Nonnull
    String getUrl();

    /**
     * 获取请求方法
     *
     * @return 请求方法
     */
    @Nonnull
    HttpMethod getMethod();

    /**
     * 获取请求类型
     *
     * @return 请求类型
     */
    @Nullable
    MediaType getContentType();

    /**
     * 获取全部请求头
     * @return 请求头
     */
    @Nonnull
    List<Header> getHeaders();

    /**
     * 获取URL查询参数,如:
     * <ul>
     *     <li>/path?a=1&b=2 -> {"a":"1","b":"2"}</li>
     *     <li>/path?a=1&b=2&b=3 -> {"a":"1","b":"2,3"}</li>
     * </ul>
     * @return 查询参数
     */
    @Nullable
    Map<String, String> getQueryParameters();

    /**
     * 获取表单参数,通常针对 POST application/x-www-form-urlencoded 请求
     * @return 表单参数
     */
    @Nullable
    default Map<String, String> getRequestParam() {
        if (MediaType.APPLICATION_FORM_URLENCODED.includes(getContentType())) {
            return HttpUtils.parseEncodedUrlParams(payloadAsString());
        }
        return Collections.emptyMap();
    }

    default Optional<MultiPart> multiPart() {
        return Optional.empty();
    }

    default Object parseBody() {
        if (MediaType.APPLICATION_JSON.includes(getContentType())) {
            return JSON.parse(payloadAsBytes());
        }

        if (MediaType.APPLICATION_FORM_URLENCODED.includes(getContentType())) {
            return HttpUtils.parseEncodedUrlParams(payloadAsString());
        }

        return payloadAsString();
    }

    default Optional<Header> getHeader(String name) {
        return getHeaders()
            .stream()
            .filter(header -> header.getName().equals(name))
            .findFirst();
    }

    default Optional<String> getQueryParameter(String name) {
        return Optional.ofNullable(getQueryParameters())
                       .map(map -> map.get(name));
    }

    default String print() {
        StringBuilder builder = new StringBuilder();
        builder.append(getMethod()).append(" ").append(getPath());
        if (!CollectionUtils.isEmpty(getQueryParameters())) {
            builder.append("?")
                   .append(getQueryParameters()
                               .entrySet().stream()
                               .map(e -> e.getKey().concat("=").concat(e.getValue()))
                               .collect(Collectors.joining("&")))
                   .append("\n");
        } else {
            builder.append("\n");
        }
        for (Header header : getHeaders()) {
            builder
                .append(header.getName()).append(": ").append(String.join(",", header.getValue()))
                .append("\n");
        }
        if (multiPart().isPresent()) {
            multiPart().ifPresent(parts -> {
                builder.append("\n");
                for (Part part : parts.getParts()) {
                    builder.append(part).append("\n");
                }
            });
        } else {
            ByteBuf payload = getPayload();
            if (payload.readableBytes() == 0) {
                return builder.toString();
            }
            builder.append("\n");
            if (ByteBufUtil.isText(payload, StandardCharsets.UTF_8)) {
                builder.append(payload.toString(StandardCharsets.UTF_8));
            } else {
                ByteBufUtil.appendPrettyHexDump(builder, payload);
            }
        }
        return builder.toString();
    }

}
