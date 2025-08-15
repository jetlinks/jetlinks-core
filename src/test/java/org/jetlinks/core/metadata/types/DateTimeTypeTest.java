package org.jetlinks.core.metadata.types;

import org.junit.Assert;
import org.junit.Test;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Date;

import static org.junit.Assert.assertEquals;


public class DateTimeTypeTest  extends JsonableTestBase<DateTimeType> {

    @Override
    protected DateTimeType newInstance() {
        return new DateTimeType();
    }

    @Override
    protected void fillSampleData(DateTimeType instance) {
        instance.setFormat("yyyy-MM-dd HH:mm:ss");
        instance.setZoneId(ZoneId.of("Asia/Shanghai"));
    }

    @Override
    protected void assertSampleData(DateTimeType instance) {
        assertEquals("yyyy-MM-dd HH:mm:ss", instance.getFormat());
        assertEquals(ZoneId.of("Asia/Shanghai"), instance.getZoneId());
    }

    @Test
    public void test() {
        DateTimeType timeType = new DateTimeType();
        timeType.setZoneId(ZoneId.of("Asia/Shanghai"));
        Assert.assertTrue(timeType.validate(System.currentTimeMillis()).isSuccess());


        Assert.assertNotNull(timeType.format(System.currentTimeMillis()));

        timeType.setFormat("yyyy-MM-dd");

        assertEquals(timeType.format(Date.from(LocalDateTime.of(2019, 8, 10, 0, 0)
                        .toInstant(ZoneOffset.of("+8")))),
                "2019-08-10");


    }

    @Test
    public void testTime() {

        DateTimeType timeType = new DateTimeType();
//        timeType.setFormat("yyyy-MM-dd'T'HH:mm:ss.SSSz");
        Date date = timeType.convert("2020-01-01T09:48:10.876+08:00");

        System.out.println(date);

    }


    @Test
    public void testString(){

        DateTimeType.GLOBAL.convert("1600262329000");

    }

    @Test
    public void testTimestamp() {

        long ts = 1597372303947L;

        DateTimeType timeType = new DateTimeType();
        Date date = timeType.convert(""+ts);

       assertEquals(date.getTime(),ts);

    }
}