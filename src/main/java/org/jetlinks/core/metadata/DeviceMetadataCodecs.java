package org.jetlinks.core.metadata;

import lombok.extern.slf4j.Slf4j;

import java.util.ServiceLoader;

@Slf4j
public class DeviceMetadataCodecs {

    private static DeviceMetadataCodec codec;

    static {
        ServiceLoader<DeviceMetadataCodec> loader = ServiceLoader.load(DeviceMetadataCodec.class);
        for (DeviceMetadataCodec metadataCodec : loader) {
            codec = metadataCodec;
            log.debug("load default DeviceMetadataCodec {}", metadataCodec);
            break;
        }
        if (codec == null) {
            log.warn("no default DeviceMetadataCodec load");
        }
    }


    public static DeviceMetadataCodec defaultCodec() {
        return codec;
    }
}
