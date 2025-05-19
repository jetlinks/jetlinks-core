package org.jetlinks.core.command.context;

import lombok.AllArgsConstructor;
import org.jetlinks.core.command.CommandSupport;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.Objects;
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
    public boolean isSupported(String name) {
        return Objects.equals(this.name, name);
    }

    @Nonnull
    @Override
    public Set<String> getSupports() {
        return Collections.singleton(name);
    }
}
