package org.jetlinks.core.utils;

import io.netty.util.concurrent.FastThreadLocal;
import org.jetlinks.core.lang.SeparatedCharSequence;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.PathMatcher;

import java.util.*;

public class TopicUtils {
    public static final char PATH_SPLITTER = '/';

    public static final String ANY = "*";
    public static final String ANY_ALL = "**";


    private final static PathMatcher pathMatcher = new AntPathMatcher();

    private final static Map<String, String[]> splitCache;

    static {
        splitCache = new ConcurrentReferenceHashMap<>(
            20480,
            ConcurrentReferenceHashMap.ReferenceType.WEAK);
    }

    /**
     * 将url转为mqtt topic,支持通配符转转换
     * <pre>
     *    /user/* => /user/+
     *    /user/** => /user/#
     *    /user/{uid}/msg => /user/+/msg
     * </pre>
     *
     * @param url URL
     * @return topic
     */
    public static String convertToMqttTopic(String url) {
        String[] arr = split(url);
        for (int i = 0; i < arr.length; i++) {
            String str = arr[i];
            if (str.startsWith("{") && str.endsWith("}")) {
                if (str.charAt(1) == '#') {
                    arr[i] = "#";
                } else {
                    arr[i] = "+";
                }
            } else if (str.equals("**")) {
                arr[i] = "#";
            } else if (str.equals("*")) {
                arr[i] = "+";
            }
        }
        return String.join("/", arr);
    }

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
        return split(topic, false);
    }

    public static String[] split(String topic, boolean cache, boolean intern) {

        if (!cache) {
            return doSplit(topic);
        }

        if (intern) {
            return splitCache.computeIfAbsent(
                topic,
                t -> {
                    String[] arr = doSplit(t);
                    for (int i = 0; i < arr.length; i++) {
                        if (Objects.equals(arr[i], ANY)) {
                            arr[i] = ANY;
                        } else if (Objects.equals(arr[i], ANY_ALL)) {
                            arr[i] = ANY_ALL;
                        } else {
                            arr[i] = RecyclerUtils.intern(arr[i]);
                        }
                    }
                    return arr;
                });
        }

        return splitCache.computeIfAbsent(topic, TopicUtils::doSplit);
    }

    private static final FastThreadLocal<List<String>> SHARE_SPLIT = new FastThreadLocal<List<String>>() {
        @Override
        protected List<String> initialValue() {
            return new ArrayList<>(8);
        }
    };

    private static final FastThreadLocal<StringBuilder> SHARE_BUILDER = new FastThreadLocal<StringBuilder>() {
        @Override
        protected StringBuilder initialValue() {
            return new StringBuilder(8);
        }
    };

    private static String[] doSplit(String topic) {
        return split(topic, PATH_SPLITTER);
    }

    public static String[] split(String topic, char pattern) {
        List<String> list = SHARE_SPLIT.get();
        StringBuilder builder = SHARE_BUILDER.get();
        try {
            int len = topic.length();
            int total = 0;

            for (int i = 0; i < len; i++) {
                char ch = topic.charAt(i);
                if (ch == pattern) {
                    list.add(builder.toString());
                    builder.setLength(0);
                    total++;
                } else {
                    builder.append(ch);
                }
            }

            if (builder.length() > 0) {
                list.add(builder.toString());
                total++;
            }

            return list.toArray(new String[total]);
        } finally {
            builder.setLength(0);
            list.clear();
        }
    }


    public static String[] split(String topic, boolean cache) {
        return split(topic, cache, true);
    }

    private static boolean matchStrings(String str, String pattern) {
        return str.equals(pattern)
            || "*".equals(pattern)
            || "*".equals(str);
    }

    private static boolean matchStrings(CharSequence str, CharSequence pattern) {
        return pattern.equals(str)
            || pattern.equals("*")
            || str.equals("*");
    }

    public static boolean match(SeparatedCharSequence pattern, SeparatedCharSequence topicParts) {
        int patternSize=pattern.size();
        int partsSize = topicParts.size();
        if (patternSize == 0 && partsSize == 0) {
            return true;
        }
        int pattIdxStart = 0;
        int pattIdxEnd = patternSize - 1;
        int pathIdxStart = 0;
        int pathIdxEnd = partsSize- 1;
        while (pattIdxStart <= pattIdxEnd && pathIdxStart <= pathIdxEnd) {
            CharSequence pattDir = pattern.get(pattIdxStart);
            //匹配多层
            if (pattDir.equals("**")) {
                break;
            }
            if (!matchStrings(pattDir, topicParts.get(pathIdxStart))) {
                return false;
            }
            pattIdxStart++;
            pathIdxStart++;
        }
        if (pathIdxStart > pathIdxEnd) {
            if (pattIdxStart > pattIdxEnd) {
                return (pattern.get(patternSize - 1).equals("/") == topicParts.get(partsSize - 1).equals("/"));
            }

            if (pattIdxStart == pattIdxEnd && pattern.get(pattIdxStart).equals("*") && topicParts.get(partsSize - 1).equals("/")) {
                return true;
            }
            for (int i = pattIdxStart; i <= pattIdxEnd; i++) {
                if (!pattern.get(i).equals("**")) {
                    return false;
                }
            }
            return true;
        } else if (pattIdxStart > pattIdxEnd) {
            // String not exhausted, but pattern is. Failure.
            return false;
        } else if (topicParts.get(pattIdxStart).equals("**")) {
            // Path start definitely matches due to "**" part in pattern.
            return true;
        }
        // up to last '**'
        while (pattIdxStart <= pattIdxEnd && pathIdxStart <= pathIdxEnd) {
            CharSequence pattDir = pattern.get(pattIdxEnd);
            if (pattDir.equals("**")) {
                break;
            }
            if (!matchStrings(pattDir, topicParts.get(pathIdxEnd))) {
                return false;
            }
            pattIdxEnd--;
            pathIdxEnd--;
        }
        if (pathIdxStart > pathIdxEnd) {
            // String is exhausted
            for (int i = pattIdxStart; i <= pattIdxEnd; i++) {
                if (!pattern.get(i).equals("**")) {
                    return false;
                }
            }
            return true;
        }

        while (pattIdxStart != pattIdxEnd && pathIdxStart <= pathIdxEnd) {
            int patIdxTmp = -1;
            for (int i = pattIdxStart + 1; i <= pattIdxEnd; i++) {
                if (pattern.get(i).equals("**")) {
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
                    CharSequence subPat = pattern.get(pattIdxStart + j + 1);
                    CharSequence subStr = topicParts.get(pathIdxStart + i + j);
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
            if (!pattern.get(i).equals("**")) {
                return false;
            }
        }
        return true;
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
     * <p>
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
        if (!topic.contains(",") && !topic.contains("{")) {
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
