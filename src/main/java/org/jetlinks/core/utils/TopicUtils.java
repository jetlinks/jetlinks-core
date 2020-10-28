package org.jetlinks.core.utils;

import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;

import java.util.*;

public class TopicUtils {

    private final static PathMatcher pathMatcher = new AntPathMatcher();

    public static boolean match(String topic, String target) {

        if (topic.equals(target)) {
            return true;
        }

        if (!topic.contains("*")
                && !topic.contains("#") && !topic.contains("+")
                && !topic.contains("{")) {
            return false;
        }

        return pathMatcher.match(topic.replace("#", "**").replace("+", "*"), target);

    }

    public static Map<String, String> getPathVariables(String template, String topic) {
        try {
            return pathMatcher.extractUriTemplateVariables(template, topic);
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }

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
