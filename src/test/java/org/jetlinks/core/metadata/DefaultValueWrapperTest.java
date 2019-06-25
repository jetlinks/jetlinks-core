package org.jetlinks.core.metadata;

import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.*;

public class DefaultValueWrapperTest {

    @Test
    public void testInt() {

        ValueWrapper valueWrapper=new DefaultValueWrapper(Long.MAX_VALUE);
        Assert.assertTrue(valueWrapper.asInteger().isPresent());


    }

}