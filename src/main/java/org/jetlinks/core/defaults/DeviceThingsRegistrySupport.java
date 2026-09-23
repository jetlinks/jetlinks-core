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

    public void checkThingType(String type) {
        if (!DeviceThingType.device.getId().equals(type)) {
            throw new UnsupportedOperationException("unsupported thing type:" + type);
        }
    }

    @Override
    public Mono<Thing> getThing(@Nonnull String thingType, @Nonnull String thingId) {
        checkThingType(thingType);
        return Mono.from(registry.getDevice(thingId));
    }

    @Override
    public Mono<ThingTemplate> getTemplate(@Nonnull String thingType, @Nonnull String templateId) {
        return Mono.from(registry.getProduct(templateId));
    }

    @Override
    public Mono<Thing> register(@Nonnull String thingType, @Nonnull ThingInfo info) {
        checkThingType(thingType);

        return Mono.from(
            registry.register(DeviceInfo.builder()
                                        .id(info.getId())
                                        .productId(info.getTemplateId())
                                        .metadata(info.getMetadata())
                                        .configuration(info.getConfiguration())
                                        .build())
        );
    }

    @Override
    public Mono<Void> unregisterThing(@Nonnull String thingType, @Nonnull String thingId) {
        checkThingType(thingType);
        return registry.unregisterDevice(thingId);
    }

    @Override
    public Mono<ThingTemplate> register(@Nonnull String thingType, @Nonnull ThingTemplateInfo templateInfo) {
        checkThingType(thingType);
        return Mono.from(
            registry.register(ProductInfo.builder()
                                         .id(templateInfo.getId())
                                         .metadata(templateInfo.getMetadata())
                                         .configuration(templateInfo.getConfiguration())
                                         .build())
        );
    }

    @Override
    public Mono<Void> unregisterTemplate(@Nonnull String thingType, @Nonnull String templateId) {
        checkThingType(thingType);
        return registry.unregisterProduct(templateId);
    }

    @Override
    public boolean isSupported(String thingType) {
        return DeviceThingType.device.getId().equals(thingType);
    }
}
