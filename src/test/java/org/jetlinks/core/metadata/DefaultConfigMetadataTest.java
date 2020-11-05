package org.jetlinks.core.metadata;

import org.jetlinks.core.metadata.types.StringType;
import org.junit.Test;

import static org.junit.Assert.*;

public class DefaultConfigMetadataTest {


    @Test
    public void test() {
        DefaultConfigMetadata metadata = new DefaultConfigMetadata();
        metadata.add("test", "test", StringType.GLOBAL, DeviceConfigScope.device);
        metadata.add("test2", "test2", StringType.GLOBAL, DeviceConfigScope.product);

        ConfigMetadata newConf = metadata.copy(DeviceConfigScope.device);

        assertEquals(1, newConf.getProperties().size());
        assertEquals(newConf.getProperties().get(0).getProperty(),"test");
    }

}