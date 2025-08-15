package org.jetlinks.core.metadata.types;

import org.junit.Test;

import static org.junit.Assert.*;

public class IntTypeTest extends JsonableTestBase.Empty<IntType> {

    @Override
    protected IntType newInstance() {
        return new IntType();
    }

    @Test
    public void testFloat() {
        assertEquals(Integer.valueOf(1), IntType.GLOBAL.convert("1.23"));
    }
}