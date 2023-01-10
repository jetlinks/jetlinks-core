package org.jetlinks.core.message.codec.http;

import lombok.SneakyThrows;
import org.apache.commons.codec.net.URLCodec;
import org.springframework.util.StringUtils;

import java.net.URL;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class HttpUtils {

    static final URLCodec urlCodec = new URLCodec();

    @SneakyThrows
    public static String urlDecode(String url) {
        return urlCodec.decode(url);
    }

    @SneakyThrows
    public static String urlEncode(String url) {
        return urlCodec.encode(url);
    }

    @SneakyThrows
    public static String getUrlPath(String url) {
        String path;
        if (!url.contains("://")) {
            if (!url.startsWith("/")) {
                url = "/" + url;
            }
            url = "http://test" + url;
        }
        path = new URL(url).getPath();
        if (!path.startsWith("/")) {
            return "/".concat(path);
        }
        return path;
    }

    public static String createEncodedUrlParams(Map<?, ?> maps) {
        return maps
                .entrySet()
                .stream()
                .map(e -> urlEncode(String.valueOf(e.getKey())) + "=" + urlEncode(String.valueOf(e.getValue())))
                .collect(Collectors.joining("&"));

    }

    public static Map<String, String> parseEncodedUrlParams(String uriOrQuery) {
        return parseEncodedUrlParams(uriOrQuery, LinkedHashMap::new);
    }

    public static Map<String, String> parseEncodedUrlParams(String uriOrQuery, Supplier<Map<String, String>> supplier) {
        if (StringUtils.isEmpty(uriOrQuery)) {
            return Collections.emptyMap();
        }
        int queryIndex = uriOrQuery.indexOf("?");
        if (queryIndex >= 0) {
            uriOrQuery = uriOrQuery.substring(queryIndex+1);
        }
        return Stream
                .of(uriOrQuery.split("[&]"))
                .map(par -> par.split("[=]", 2))
                .collect(
                        Collectors.toMap(
                                arr -> urlDecode(arr[0]),  // key
                                arr -> arr.length > 1 ? urlDecode(arr[1]) : "", // value
                                (a, b) -> String.join(",", a, b),// 合并相同value
                                supplier
                        )
                );
    }

    public static String appendUrlParameter(String url, Map<?, ?> param) {
        return appendUrlParameter(url, createEncodedUrlParams(param));
    }

    public static String appendUrlParameter(String url, String param) {
        if (StringUtils.isEmpty(url)) {
            return url;
        }
        if (url.contains("?")) {
            return url + "&" + param;
        } else {
            return url + "?" + param;
        }
    }

}
