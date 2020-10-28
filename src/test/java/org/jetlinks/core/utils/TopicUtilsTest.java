package org.jetlinks.core.utils;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class TopicUtilsTest {

    @Test
    public void test() {

        List<String> expands = TopicUtils.expand("/1,2,3");

        assertEquals(expands.size(),3);
        assertEquals(expands.get(0),"/1");
        assertEquals(expands.get(1),"/2");
        assertEquals(expands.get(2),"/3");

    }
    @Test
    public void test1() {

        List<String> expands = TopicUtils.expand("/1,2,3/test/test2");

        assertEquals(expands.size(),3);
        assertEquals(expands.get(0),"/1/test/test2");
        assertEquals(expands.get(1),"/2/test/test2");
        assertEquals(expands.get(2),"/3/test/test2");

    }
    @Test
    public void test2() {

        List<String> expands = TopicUtils.expand("/test/1,2,3/test2");

        assertEquals(expands.size(),3);
        assertEquals(expands.get(0),"/test/1/test2");
        assertEquals(expands.get(1),"/test/2/test2");
        assertEquals(expands.get(2),"/test/3/test2");

    }

    @Test
    public void test3() {

        List<String> expands = TopicUtils.expand("/test/1,2/1,2");

        assertEquals(expands.size(),4);
        assertEquals(expands.get(0),"/test/1/1");
        assertEquals(expands.get(1),"/test/1/2");
        assertEquals(expands.get(2),"/test/2/1");
        assertEquals(expands.get(3),"/test/2/2");

    }
}