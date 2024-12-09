package org.jetlinks.core.lang;

import org.junit.Test;

import static org.junit.Assert.*;

public class SharedPathStringTest {


    @Test
    public void test(){
        String topic = "/test/1/2/3";

        SharedPathString sharedPathString = SharedPathString.of(topic);
        assertEquals(topic, sharedPathString.toString());
        assertEquals(topic.length(), sharedPathString.length());
        assertEquals(topic.charAt(0), sharedPathString.charAt(0));
        assertEquals(topic.substring(1,2), sharedPathString.subSequence(1, 2).toString());

        System.out.println(sharedPathString.hashCode());

    }
}