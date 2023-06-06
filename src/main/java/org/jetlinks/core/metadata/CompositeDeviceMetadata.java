package org.jetlinks.core.metadata;

import org.jetlinks.core.things.ThingMetadata;

public class CompositeDeviceMetadata extends CompositeThingMetadata implements DeviceMetadata {

    public CompositeDeviceMetadata(ThingMetadata main, ThingMetadata right) {
        super(main, right);
    }

    @Override
    public <T extends ThingMetadata> CompositeDeviceMetadata merge(T metadata) {
        return this.merge(metadata, MergeOption.DEFAULT_OPTIONS);
    }

    @Override
    public <T extends ThingMetadata> CompositeDeviceMetadata merge(T metadata, MergeOption... options) {
        return new CompositeDeviceMetadata(left.merge(metadata, options), right);
    }
}
