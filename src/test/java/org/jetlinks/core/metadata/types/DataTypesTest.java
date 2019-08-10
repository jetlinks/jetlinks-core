package org.jetlinks.core.metadata.types;

import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.*;

public class DataTypesTest {

    @Test
    public void testLookup(){
        Assert.assertNotNull(DataTypes.lookup("int"));
        Assert.assertNotNull(DataTypes.lookup("long"));
        Assert.assertNotNull(DataTypes.lookup("double"));
        Assert.assertNotNull(DataTypes.lookup("float"));
        Assert.assertNotNull(DataTypes.lookup("date"));
        Assert.assertNotNull(DataTypes.lookup("object"));
        Assert.assertNotNull(DataTypes.lookup("array"));
        Assert.assertNotNull(DataTypes.lookup("enum"));

    }
}