package org.jetlinks.core.command.context;

import lombok.AllArgsConstructor;
import org.jetlinks.core.command.CommandSupport;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.Set;

@AllArgsConstructor
class SingleCommandContext implements CommandContext {

    private final String name;
    private final Mono<CommandSupport> commandSupport;

    @Override
    public Mono<CommandSupport> getCommandSupport(String name) {
        return commandSupport;
    }

    @Override
    public Set<String> getSupports() {
        return Collections.singleton(name);
    }
}
