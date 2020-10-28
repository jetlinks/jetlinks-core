package org.jetlinks.core.utils;

import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;

import java.util.*;

public class TopicUtils {

    private final static PathMatcher pathMatcher = new AntPathMatcher();

    /**
     * 匹配topic
     *
     * <pre>
     *     match("/test/*","/test/1"); // true
     *     match("/test/*","/test/1/2"); // false
     *     match("/test/**","/test/1/2"); // true
     * </pre>
     *
     * @param pattern 匹配模版
     * @param topic   要匹配的topic
     * @return 是否匹配
     */
    public static boolean match(String pattern, String topic) {

        if (pattern.equals(topic)) {
            return true;
        }

        if (!pattern.contains("*")
                && !pattern.contains("#") && !pattern.contains("+")
                && !pattern.contains("{")) {
            return false;
        }

        return pathMatcher.match(pattern.replace("#", "**").replace("+", "*"), topic);

    }

    /**
     * 根据模版从url上提取变量,如果提取出错则返回空Map
     *
     * <pre>
     *   getPathVariables("/device/{productId}","/device/test123");
     *   => {"productId","test1234"}
     * </pre>
     *
     * @param template 模版
     * @param topic    要提取的topic
     * @return 提取结果
     */
    public static Map<String, String> getPathVariables(String template, String topic) {
        try {
            return pathMatcher.extractUriTemplateVariables(template, topic);
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }

    /**
     * 展开topic
     * <p>
     * before:
     * <pre>
     *      /device/a,b,v/*
     *  </pre>
     * after:
     * <pre>
     *     /device/a/*
     *     /device/b/*
     *     /device/v/*
     * </pre>
     *
     * @param topic topic
     * @return 展开的topic集合
     */
    public static List<String> expand(String topic) {
        if (!topic.contains(",")) {
            return Collections.singletonList(topic);
        }
        if (topic.startsWith("/")) {
            topic = topic.substring(1);
        }
        String[] parts = topic.split("/", 2);

        String first = parts[0];
        List<String> expands = new ArrayList<>();

        if (parts.length == 1) {
            for (String split : first.split(",")) {
                expands.add("/" + split);
            }
            return expands;
        }

        List<String> nextTopics = expand(parts[1]);

        for (String split : first.split(",")) {

            for (String nextTopic : nextTopics) {
                StringJoiner joiner = new StringJoiner("");
                joiner.add("/");
                joiner.add(split);
                if (!nextTopic.startsWith("/")) {
                    joiner.add("/");
                }
                joiner.add(nextTopic);
                expands.add(joiner.toString());
            }

        }

        return expands;
    }

}
