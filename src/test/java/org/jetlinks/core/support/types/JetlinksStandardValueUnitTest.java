package org.jetlinks.core.support.types;

import org.jetlinks.core.metadata.unit.MeasurementUnit;
import org.junit.Test;

import static org.junit.Assert.*;

public class JetlinksStandardValueUnitTest {

    @Test
    public void test() {

        //from StandardUnit
        JetlinksStandardValueUnit unit = JetlinksStandardValueUnit.of(MeasurementUnit.meter);

        assertNotNull(unit);
        assertEquals(unit.format("1"), "1m");

        //from json
        JetlinksStandardValueUnit unit2 = JetlinksStandardValueUnit.of(unit.toJson());
        assertNotNull(unit2);
        assertEquals(unit2.format("1"), "1m");

        //from id
        JetlinksStandardValueUnit unit3 = JetlinksStandardValueUnit.of(MeasurementUnit.meter.getId());
        assertNotNull(unit3);
        assertEquals(unit3.format("1"), "1m");
    }

}