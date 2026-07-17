package org.jetlinks.core.defaults;

import org.jetlinks.core.device.DeviceFeatures;
import org.jetlinks.core.message.codec.DefaultTransport;
import org.jetlinks.core.metadata.FirmwareMetadata;
import org.jetlinks.core.metadata.FirmwareMetadataContext;
import org.junit.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Optional;

import static org.junit.Assert.assertSame;

public class CompositeProtocolSupportTest {

    @Test
    public void testAddFirmwareSupport() {
        CompositeProtocolSupport support = new CompositeProtocolSupport();
        FirmwareMetadata metadata = new FirmwareMetadata();
        metadata.setVersion("1.0.0");
        FirmwareMetadataContext context = new FirmwareMetadataContext() {
            @Override
            public String getFileLocation() {
                return "test.zip";
            }

            @Override
            public <T> T readContent(ContentReader<T> reader) {
                throw new AssertionError("parser controls firmware content access");
            }

            @Override
            public <T> Optional<T> readArchive(ArchiveReader<T> reader) {
                throw new AssertionError("parser controls firmware archive access");
            }
        };

        support.addFirmwareSupport(actual -> {
            assertSame(context, actual);
            return Mono.just(metadata);
        });

        StepVerifier
            .create(support.parseFirmwareMetadata(context))
            .expectNext(metadata)
            .verifyComplete();
        StepVerifier
            .create(support.getFeatures(DefaultTransport.MQTT))
            .expectNext(DeviceFeatures.supportFirmware)
            .verifyComplete();
    }
}
