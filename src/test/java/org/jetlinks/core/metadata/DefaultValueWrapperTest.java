package org.jetlinks.core.metadata;

import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.*;

public class DefaultValueWrapperTest {

    @Test
    public void testInt() {

        ValueWrapper valueWrapper = new DefaultValueWrapper(Long.MAX_VALUE);
        Assert.assertTrue(valueWrapper.asInteger().isPresent());

        Assert.assertTrue(valueWrapper.as(Long.class).isPresent());

    }

    @Test
    public void testList() {

        ValueWrapper valueWrapper = new DefaultValueWrapper("[1,2,3]");
        int val = valueWrapper.asList(Integer.class)
                .map(list -> list.stream().mapToInt(Integer::intValue).sum())
                .orElse(0);

        Assert.assertEquals(val, 6);

    }

}