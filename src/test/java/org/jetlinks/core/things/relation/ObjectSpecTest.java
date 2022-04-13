package org.jetlinks.core.things.relation;


import org.junit.Assert;
import org.junit.Test;

public class ObjectSpecTest {

    @Test
    public void test() {

        ObjectSpec spec = ObjectSpec.parse("dev1@device:manager@user:member@user");
        Assert.assertNotNull(spec);

        System.out.printf(spec.toString());

    }
}