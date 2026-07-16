package org.jetlinks.core;

import org.jetlinks.core.device.TestProtocolSupport;
import org.jetlinks.core.metadata.FirmwareMetadata;
import org.jetlinks.core.metadata.FirmwareMetadataContext;
import org.junit.Test;
import reactor.test.StepVerifier;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.Assert.assertEquals;

public class ProtocolSupportTest {

    @Test
    public void testParseFirmwareMetadataDefault() {
        FirmwareMetadataContext context = new FirmwareMetadataContext() {
            @Override
            public String getFileLocation() {
                return "test.bin";
            }

            @Override
            public <T> T readContent(ContentReader<T> reader) {
                throw new AssertionError("default implementation must not read firmware content");
            }

            @Override
            public <T> Optional<T> readArchive(ArchiveReader<T> reader) {
                throw new AssertionError("default implementation must not read firmware archive");
            }
        };
        StepVerifier
            .create(new TestProtocolSupport().parseFirmwareMetadata(context))
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
