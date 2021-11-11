package org.jetlinks.core.things;

import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.function.Function3;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;

public class DefaultThingsRegistry implements ThingsRegistry {

    private final List<ThingsRegistrySupport> supports = new CopyOnWriteArrayList<>();

    public DefaultThingsRegistry() {

    }

    public DefaultThingsRegistry(Iterable<ThingsRegistrySupport> supports) {
        for (ThingsRegistrySupport support : supports) {
            this.supports.add(support);
        }
    }

    public Disposable addSupport(ThingsRegistrySupport support) {
        supports.add(support);
        return () -> supports.remove(support);
    }

    protected <ARG, R> R findSupport(String thingType,
                                     ARG arg,
                                     Function3<ThingsRegistrySupport, String, ARG, R> computer,
                                     Supplier<R> defaultGetter) {
        for (ThingsRegistrySupport support : supports) {
            if (support.isSupported(thingType)) {
                return computer.apply(support, thingType, arg);
            }
        }
        return defaultGetter.get();
    }

    @Override
    public Mono<Thing> getThing(@Nonnull String thingType, @Nonnull String thingId) {
        return findSupport(thingType, thingId, ThingsRegistrySupport::getThing, Mono::empty);
    }

    @Override
    public Mono<ThingTemplate> getTemplate(@Nonnull String thingType, @Nonnull String templateId) {
        return findSupport(thingType, templateId, ThingsRegistrySupport::getTemplate, Mono::empty);
    }

    @Override
    public Mono<Thing> register(@Nonnull String thingType, @Nonnull ThingInfo info) {
        return findSupport(thingType, info, ThingsRegistrySupport::register, Mono::empty);
    }

    @Override
    public Mono<Void> unregisterThing(@Nonnull String thingType, @Nonnull String thingId) {
        return findSupport(thingType, thingId, ThingsRegistrySupport::unregisterThing, Mono::empty);
    }

    @Override
    public Mono<ThingTemplate> register(@Nonnull String thingType, @Nonnull ThingTemplateInfo templateInfo) {
        return findSupport(thingType, templateInfo, ThingsRegistrySupport::register, Mono::empty);
    }

    @Override
    public Mono<Void> unregisterTemplate(@Nonnull String thingType, @Nonnull String thingId) {
        return findSupport(thingType, thingId, ThingsRegistrySupport::unregisterTemplate, Mono::empty);
    }
}
