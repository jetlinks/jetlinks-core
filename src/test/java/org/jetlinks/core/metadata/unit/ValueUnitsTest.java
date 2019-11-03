package org.jetlinks.core.metadata.unit;

import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.*;

public class ValueUnitsTest {
    @Test
    public void test(){
        Assert.assertNotNull(ValueUnits.lookup("meter"));

        Assert.assertFalse(ValueUnits.getAllUnit().isEmpty());
    }
}