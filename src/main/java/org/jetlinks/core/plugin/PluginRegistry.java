package org.jetlinks.core.plugin;

import reactor.core.publisher.Mono;

public interface PluginRegistry {

    Mono<Plugin> getPlugin(String type,String id);

}
