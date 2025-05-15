package org.jetlinks.core.command.context;

import lombok.AllArgsConstructor;
import org.jetlinks.core.command.CommandSupport;
import org.jetlinks.core.utils.CompositeSet;
import reactor.core.publisher.Mono;

import java.util.Set;

@AllArgsConstructor
class CommandContextOr implements CommandContext {

    private final CommandContext left;
    private final CommandContext right;

    @Override
    public boolean isWrapperFor(Class<?> type) {
        return left.isWrapperFor(type) || right.isWrapperFor(type);
    }

    @Override
    public <T> T unwrap(Class<T> type) {
        return left.isWrapperFor(type) ? left.unwrap(type) : right.unwrap(type);
    }

    @Override
    public Mono<CommandSupport> getCommandSupport(String name) {
        return left
            .getCommandSupport(name)
            .switchIfEmpty(right.getCommandSupport(name));
    }

    @Override
    public boolean isSupported(String name) {
        return left.isSupported(name) || right.isSupported(name);
    }

    @Override
    public Set<String> getSupports() {
        return new CompositeSet<>(left.getSupports(), right.getSupports());
    }
}
