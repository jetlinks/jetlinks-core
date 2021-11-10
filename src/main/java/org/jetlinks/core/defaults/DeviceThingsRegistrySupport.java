package org.jetlinks.core.defaults;

import lombok.AllArgsConstructor;
import org.jetlinks.core.device.DeviceInfo;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.device.DeviceThingType;
import org.jetlinks.core.device.ProductInfo;
import org.jetlinks.core.things.*;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;

@AllArgsConstructor
public class DeviceThingsRegistrySupport implements ThingsRegistrySupport {

    private final DeviceRegistry registry;

    public void checkThingType(ThingType type) {
        if (!DeviceThingType.device.isSameType(type)) {
            throw new UnsupportedOperationException("unsupported thing type:" + type.getId());
        }
    }

    @Override
    public Mono<Thing> getThing(@Nonnull ThingType thingType, @Nonnull String thingId) {
        checkThingType(thingType);
        return registry
                .getDevice(thingId)
                .cast(Thing.class);
    }

    @Override
    public Mono<ThingTemplate> getTemplate(@Nonnull ThingType thingType, @Nonnull String templateId) {
        return registry
                .getProduct(templateId)
                .cast(ThingTemplate.class);
    }

    @Override
    public Mono<Thing> register(@Nonnull ThingType thingType, @Nonnull ThingInfo info) {
        checkThingType(thingType);

        return registry
                .register(DeviceInfo.builder()
                                    .id(info.getId())
                                    .productId(info.getTemplateId())
                                    .metadata(info.getMetadata())
                                    .configuration(info.getConfiguration())
                                    .build())
                .cast(Thing.class);
    }

    @Override
    public Mono<Void> unregisterThing(@Nonnull ThingType thingType, @Nonnull String thingId) {
        checkThingType(thingType);
        return registry.unregisterDevice(thingId);
    }

    @Override
    public Mono<ThingTemplate> register(@Nonnull ThingType thingType, @Nonnull ThingTemplateInfo templateInfo) {
        checkThingType(thingType);
        return registry
                .register(ProductInfo.builder()
                                     .id(templateInfo.getId())
                                     .metadata(templateInfo.getMetadata())
                                     .configuration(templateInfo.getConfiguration())
                                     .build())
                .cast(ThingTemplate.class);
    }

    @Override
    public Mono<Void> unregisterTemplate(@Nonnull ThingType thingType, @Nonnull String templateId) {
        checkThingType(thingType);
        return registry.unregisterProduct(templateId);
    }

    @Override
    public boolean isSupported(ThingType thingType) {
        return DeviceThingType.device.isSameType(thingType);
    }
}
