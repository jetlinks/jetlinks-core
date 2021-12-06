package org.jetlinks.core.plugin;

import org.jetlinks.core.spi.ServiceContext;
import reactor.core.publisher.Mono;

public interface PluginProvider {

    String getType();

    Mono<Plugin> createPlugin(ServiceContext context);

}
