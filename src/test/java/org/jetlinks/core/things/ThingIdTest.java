package org.jetlinks.core.things;

import org.junit.Test;

import static org.junit.Assert.*;

public class ThingIdTest {

    @Test
    public void test(){
        ThingId one =  ThingId.of("test","1");
        ThingId two =  ThingId.of("test","1");
        System.out.println(one.toUniqueId());
        assertEquals(one,two);
        assertEquals(one.hashCode(),two.hashCode());
        assertEquals(one.toUniqueId(), two.toUniqueId());

    }
}