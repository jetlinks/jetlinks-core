package org.jetlinks.core.support.types;

import org.jetlinks.core.metadata.unit.UnifyUnit;
import org.junit.Test;

import static org.junit.Assert.*;

public class JetLinksStandardValueUnitTest {

    @Test
    public void test() {

        //from StandardUnit
        JetLinksStandardValueUnit unit = JetLinksStandardValueUnit.of(UnifyUnit.meter);

        assertNotNull(unit);
        assertEquals(unit.format("1"), "1m");

        //from json
        JetLinksStandardValueUnit unit2 = JetLinksStandardValueUnit.of(unit.toJson());
        assertNotNull(unit2);
        assertEquals(unit2.format("1"), "1m");

        //from id
        JetLinksStandardValueUnit unit3 = JetLinksStandardValueUnit.of(UnifyUnit.meter.getId());
        assertNotNull(unit3);
        assertEquals(unit3.format("1"), "1m");
    }

}