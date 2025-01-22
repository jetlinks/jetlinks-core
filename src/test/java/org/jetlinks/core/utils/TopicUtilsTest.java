package org.jetlinks.core.utils;

import org.junit.Test;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class TopicUtilsTest {

    @Test
    public void test() {

        List<String> expands = TopicUtils.expand("/1,2,3");

        assertEquals(3, expands.size());
        assertEquals("/1", expands.get(0));
        assertEquals("/2", expands.get(1));
        assertEquals("/3", expands.get(2));

    }
    @Test
    public void test1() {

        List<String> expands = TopicUtils.expand("/1,2,3/test/test2");

        assertEquals(3, expands.size());
        assertEquals("/1/test/test2", expands.get(0));
        assertEquals("/2/test/test2", expands.get(1));
        assertEquals("/3/test/test2", expands.get(2));

    }
    @Test
    public void test2() {

        List<String> expands = TopicUtils.expand("/test/1,2,3/test2");

        assertEquals(3, expands.size());
        assertEquals("/test/1/test2", expands.get(0));
        assertEquals("/test/2/test2", expands.get(1));
        assertEquals("/test/3/test2", expands.get(2));

    }

    @Test
    public void testPattern() {

        List<String> expands = TopicUtils.expand("/test/{name}/test2,test3");

        assertEquals("/test/*/test2", expands.get(0));
        assertEquals("/test/*/test3", expands.get(1));

    }

    @Test
    public void test3() {

        List<String> expands = TopicUtils.expand("/test/1,2/1,2");

        assertEquals(4, expands.size());
        assertEquals("/test/1/1", expands.get(0));
        assertEquals("/test/1/2", expands.get(1));
        assertEquals("/test/2/1", expands.get(2));
        assertEquals("/test/2/2", expands.get(3));

    }

    @Test
    public void testSplit() {

        List<String> expands = Arrays.asList(TopicUtils.split("/test/1/2/3"));

        assertEquals(5, expands.size());
        assertEquals("", expands.get(0));
        assertEquals("test", expands.get(1));
        assertEquals("1", expands.get(2));
        assertEquals("2", expands.get(3));
        assertEquals("3", expands.get(4));

    }


    @Test
    public void testMatch(){
        assertTrue(
                TopicUtils.match(TopicUtils.split("/test/1/2/3"), TopicUtils.split("/test/1/2/3"))
        );

        assertTrue(
                TopicUtils.match(TopicUtils.split("/test/*/2/3"), TopicUtils.split("/test/1/2/3"))
        );

        assertTrue(
                TopicUtils.match(TopicUtils.split("/test/**/3"),TopicUtils.split("/test/1/2/3"))
        );

        assertTrue(
                TopicUtils.match(TopicUtils.split("/test/1/2/3"),TopicUtils.split("/test/*/2/3"))
        );


    }

    @Test
    public void testMqtt(){
        assertEquals(
                "/device/#",
                TopicUtils.convertToMqttTopic("/device/**")

        );

        assertEquals(
                "/device/+",
                TopicUtils.convertToMqttTopic("/device/*")

        );

        assertEquals(
                "/device/+",
                TopicUtils.convertToMqttTopic("/device/{deviceId}")
        );

        assertEquals(
                "/device/#",
                TopicUtils.convertToMqttTopic("/device/{#:后缀}")
        );


        assertEquals(
                "/device/+/+",
                TopicUtils.convertToMqttTopic("/device/{deviceId:设备ID}/{type:类型}")

        );
    }

    @Test
    public void benchmark(){
        {
            long nanos = System.nanoTime();
            for (int i = 0; i < 100_0000; i++) {
                TopicUtils.split("/test/1/2/3");
            }
            System.out.println(Duration.ofNanos(System.nanoTime()-nanos));
        }
        {
            long nanos = System.nanoTime();
            for (int i = 0; i < 100_0000; i++) {
                TopicUtils.split("/test/1/2/3",true);
            }
            System.out.println(Duration.ofNanos(System.nanoTime()-nanos));
        }
    }
}