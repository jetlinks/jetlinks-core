package org.jetlinks.core.topic;

import org.jetlinks.core.utils.TopicUtils;
import org.junit.Test;

import static org.junit.Assert.*;

public class TopicTest2 {

    @Test
    public void test() {

        String topic = "/test/1/2/3";

        Topic<String> root = Topic.createRoot();
        Topic<String> t = root.append(topic);

        String[] arr = TopicUtils.split(topic);

        assertEquals(arr.length, t.size());

        for (int i = 0; i < arr.length; i++) {
            assertEquals(arr[i], t.get(i));
        }

    }
}