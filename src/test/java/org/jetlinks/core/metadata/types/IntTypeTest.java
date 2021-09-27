package org.jetlinks.core.metadata.types;

import org.junit.Test;

import static org.junit.Assert.*;

public class IntTypeTest {

    @Test
    public void testFloat() {
        assertEquals(Integer.valueOf(1), IntType.GLOBAL.convert("1.23"));
    }
}