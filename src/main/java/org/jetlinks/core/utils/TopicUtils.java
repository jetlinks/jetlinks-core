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
     * 分隔topic
     *
     * @param topic topic
     * @return 分隔结果
     */
    public static String[] split(String topic) {
        return topic.split("/");
    }

    private static boolean matchStrings(String str, String pattern) {
        return str.equals(pattern)
                || "*".equals(pattern)
                || "*".equals(str);
    }

    public static boolean match(String[] pattern, String[] topicParts) {
        if (pattern.length == 0 && topicParts.length == 0) {
            return true;
        }
        int pattIdxStart = 0;
        int pattIdxEnd = pattern.length - 1;
        int pathIdxStart = 0;
        int pathIdxEnd = topicParts.length - 1;
        while (pattIdxStart <= pattIdxEnd && pathIdxStart <= pathIdxEnd) {
            String pattDir = pattern[pattIdxStart];
            //匹配多层
            if ("**".equals(pattDir)) {
                break;
            }
            if (!matchStrings(pattDir, topicParts[pathIdxStart])) {
                return false;
            }
            pattIdxStart++;
            pathIdxStart++;
        }
        if (pathIdxStart > pathIdxEnd) {
            if (pattIdxStart > pattIdxEnd) {
                return (pattern[pattern.length - 1].equals("/") == topicParts[topicParts.length - 1].equals("/"));
            }

            if (pattIdxStart == pattIdxEnd && pattern[pattIdxStart].equals("*") && topicParts[topicParts.length - 1].equals("/")) {
                return true;
            }
            for (int i = pattIdxStart; i <= pattIdxEnd; i++) {
                if (!pattern[i].equals("**")) {
                    return false;
                }
            }
            return true;
        } else if (pattIdxStart > pattIdxEnd) {
            // String not exhausted, but pattern is. Failure.
            return false;
        } else if ("**".equals(topicParts[pattIdxStart])) {
            // Path start definitely matches due to "**" part in pattern.
            return true;
        }
        // up to last '**'
        while (pattIdxStart <= pattIdxEnd && pathIdxStart <= pathIdxEnd) {
            String pattDir = pattern[pattIdxEnd];
            if (pattDir.equals("**")) {
                break;
            }
            if (!matchStrings(pattDir, topicParts[pathIdxEnd])) {
                return false;
            }
            pattIdxEnd--;
            pathIdxEnd--;
        }
        if (pathIdxStart > pathIdxEnd) {
            // String is exhausted
            for (int i = pattIdxStart; i <= pattIdxEnd; i++) {
                if (!pattern[i].equals("**")) {
                    return false;
                }
            }
            return true;
        }

        while (pattIdxStart != pattIdxEnd && pathIdxStart <= pathIdxEnd) {
            int patIdxTmp = -1;
            for (int i = pattIdxStart + 1; i <= pattIdxEnd; i++) {
                if (pattern[i].equals("**")) {
                    patIdxTmp = i;
                    break;
                }
            }
            if (patIdxTmp == pattIdxStart + 1) {
                // '**/**' situation, so skip one
                pattIdxStart++;
                continue;
            }
            // Find the pattern between padIdxStart & padIdxTmp in str between
            // strIdxStart & strIdxEnd
            int patLength = (patIdxTmp - pattIdxStart - 1);
            int strLength = (pathIdxEnd - pathIdxStart + 1);
            int foundIdx = -1;

            strLoop:
            for (int i = 0; i <= strLength - patLength; i++) {
                for (int j = 0; j < patLength; j++) {
                    String subPat = pattern[pattIdxStart + j + 1];
                    String subStr = topicParts[pathIdxStart + i + j];
                    if (!matchStrings(subPat, subStr)) {
                        continue strLoop;
                    }
                }
                foundIdx = pathIdxStart + i;
                break;
            }

            if (foundIdx == -1) {
                return false;
            }

            pattIdxStart = patIdxTmp;
            pathIdxStart = foundIdx + patLength;
        }

        for (int i = pattIdxStart; i <= pattIdxEnd; i++) {
            if (!pattern[i].equals("**")) {
                return false;
            }
        }
        return true;
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
     * before:
     * <pre>
     *     /device/{id}
     * </pre>
     * after:
     * <pre>
     *    /device/*
     * </pre>
     *
     * @param topic topic
     * @return 展开的topic集合
     */
    public static List<String> expand(String topic) {
        if (!topic.contains(",")&&!topic.contains("{")) {
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
                if (split.startsWith("{") && split.endsWith("}")) {
                    split = "*";
                }
                expands.add("/" + split);
            }
            return expands;
        }

        List<String> nextTopics = expand(parts[1]);

        for (String split : first.split(",")) {
            if (split.startsWith("{") && split.endsWith("}")) {
                split = "*";
            }
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
