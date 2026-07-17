package org.jetlinks.core;

import org.jetlinks.core.device.TestProtocolSupport;
import org.jetlinks.core.metadata.FirmwareMetadataContext;
import org.junit.Test;
import reactor.test.StepVerifier;

import java.util.Optional;

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
}
