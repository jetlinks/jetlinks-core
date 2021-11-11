package org.jetlinks.core.defaults;

import io.vavr.Function3;
import org.jetlinks.core.things.*;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MultiThingsRegistrySupport implements ThingsRegistrySupport {

    private final Map<String, ThingsRegistrySupport> supports = new ConcurrentHashMap<>();

    public void addSupport(ThingType type, ThingsRegistrySupport support) {
        supports.put(type.getId(), support);
    }

    public void removeSupport(ThingType type) {
        supports.remove(type.getId());
    }

    protected ThingsRegistrySupport supportNotFound(String thingType) {
        throw new UnsupportedOperationException("unsupported thing type " + thingType);
    }

    private <T, ARG> Mono<T> doWith(String thingType, ARG arg, Function3<ThingsRegistrySupport, String, ARG, Mono<T>> executor) {
        ThingsRegistrySupport support = supports.get(thingType);
        if (support == null) {
            support = supportNotFound(thingType);
        }
        return executor.apply(support, thingType, arg);
    }

    @Override
    public final Mono<Thing> getThing(@Nonnull String thingType,
                                      @Nonnull String thingId) {
        return doWith(thingType, thingId, ThingsRegistrySupport::getThing);
    }

    @Override
    public final Mono<ThingTemplate> getTemplate(@Nonnull String thingType,
                                                 @Nonnull String templateId) {
        return doWith(thingType, templateId, ThingsRegistrySupport::getTemplate);
    }

    @Override
    public final Mono<Thing> register(@Nonnull String thingType,
                                      @Nonnull ThingInfo info) {
        return doWith(thingType, info, ThingsRegistrySupport::register);
    }

    @Override
    public final Mono<Void> unregisterThing(@Nonnull String thingType,
                                            @Nonnull String thingId) {
        return doWith(thingType, thingId, ThingsRegistrySupport::unregisterThing);
    }

    @Override
    public final Mono<ThingTemplate> register(@Nonnull String thingType,
                                              @Nonnull ThingTemplateInfo templateInfo) {
        return doWith(thingType, templateInfo, ThingsRegistrySupport::register);
    }

    @Override
    public final Mono<Void> unregisterTemplate(@Nonnull String thingType,
                                               @Nonnull String templateId) {
        return doWith(thingType, templateId, ThingsRegistrySupport::unregisterTemplate);
    }

    @Override
    public boolean isSupported(String thingType) {
        return supports.containsKey(thingType);
    }
}
