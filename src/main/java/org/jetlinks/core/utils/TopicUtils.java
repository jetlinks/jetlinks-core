package org.jetlinks.core.utils;

import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;

import java.util.Collections;
import java.util.Map;

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

}
