package org.jetlinks.core.message.codec.http;

import lombok.SneakyThrows;

import java.net.URL;
import java.net.URLDecoder;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class HttpUtils {

    @SneakyThrows
    public static String urlDecode(String url) {
        return URLDecoder.decode(url, "utf-8");
    }

    @SneakyThrows
    public static String getUrlPath(String url) {
        String path;
        if (!url.contains("://")) {
            path = url;
        } else {
            path = new URL(url).getPath();
        }

        if (!path.startsWith("/")) {
            return "/".concat(path);
        }
        return path;
    }

    public static Map<String, String> parseEncodedUrlParams(String keyValueString) {
        return Stream.of(keyValueString.split("[&]"))
                .map(par -> par.split("[=]", 2))
                .collect(
                        Collectors.toMap(
                                arr -> urlDecode(arr[0]),  // key
                                arr -> arr.length > 1 ? urlDecode(arr[1]) : "", // value
                                (a, b) -> String.join(",", a, b)) // 合并相同value
                );
    }
}
