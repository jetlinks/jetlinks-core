package org.jetlinks.core;

import org.jetlinks.core.device.TestProtocolSupport;
import org.jetlinks.core.metadata.FirmwareMetadata;
import org.junit.Test;
import org.springframework.core.io.ByteArrayResource;
import reactor.test.StepVerifier;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class ProtocolSupportTest {

    @Test
    public void testParseFirmwareMetadataDefault() {
        StepVerifier
            .create(new TestProtocolSupport().parseFirmwareMetadata(new ByteArrayResource(new byte[0])))
            .verifyComplete();
    }

    @Test
    public void testFirmwareMetadata() {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("application", Map.of("releaseVersion", "2.12.0-SNAPSHOT"));
        FirmwareMetadata firmwareMetadata = new FirmwareMetadata();
        firmwareMetadata.setVersion("2.12.0-SNAPSHOT");
        firmwareMetadata.setMetadata(metadata);

        assertEquals("2.12.0-SNAPSHOT", firmwareMetadata.getVersion());
        assertEquals(metadata, firmwareMetadata.getMetadata());
    }
}
