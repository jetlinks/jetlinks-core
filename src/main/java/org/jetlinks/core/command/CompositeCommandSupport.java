package org.jetlinks.core.command;

import org.jetlinks.core.metadata.FunctionMetadata;
import org.jetlinks.core.utils.Reactors;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public class CompositeCommandSupport implements CommandSupport {

    public static CommandSupport of(AbstractCommandSupport... commands) {
        return new CompositeCommandSupport(Arrays.asList(commands));
    }

    public static CompositeCommandSupport create() {
        return new CompositeCommandSupport();
    }

    private final List<AbstractCommandSupport> supports = new CopyOnWriteArrayList<>();

    public CompositeCommandSupport() {

    }

    public CompositeCommandSupport(List<AbstractCommandSupport> supports) {
        this.supports.addAll(supports);
    }

    public List<AbstractCommandSupport> getSupports() {
        return Collections.unmodifiableList(supports);
    }

    public void register(AbstractCommandSupport support) {
        supports.add(support);
    }

    @Override
    public boolean isWrapperFor(Class<?> type) {
        for (AbstractCommandSupport support : supports) {
            if (support.isWrapperFor(type)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public <T> T unwrap(Class<T> type) {
        for (AbstractCommandSupport support : supports) {
            if (support.isWrapperFor(type)) {
                return support.unwrap(type);
            }
        }
        throw new ClassCastException(type.getName());
    }

    @Nonnull
    @Override
    public <R> R execute(@Nonnull Command<R> command) {
        for (AbstractCommandSupport support : supports) {
            boolean supported = support.commandIsSupported0(command.getCommandId());
            if (supported) {
                return support.execute(command);
            }
        }
        throw unsupportedCommand(command);
    }

    private RuntimeException unsupportedCommand(Command<?> command) {
        return new CommandException(this, null, "error.unsupported_command", null, command);
    }

    private RuntimeException unsupportedCommand(String commandId) {
        return new CommandException(this, null, "error.unsupported_command", null, commandId);
    }

    @Override
    public Flux<Object> executeToFlux(Command<?> command) {
        for (AbstractCommandSupport support : supports) {
            boolean supported = support.commandIsSupported0(command.getCommandId());
            if (supported) {
                return support.executeToFlux(command);
            }
        }
        return Flux.error(unsupportedCommand(command));
    }

    @Override
    public Flux<Object> executeToFlux(String commandId, Map<String, Object> parameters) {
        for (AbstractCommandSupport support : supports) {
            boolean supported = support.commandIsSupported0(commandId);
            if (supported) {
                return support.executeToFlux(commandId, parameters);
            }
        }
        return Flux.error(unsupportedCommand(commandId));
    }

    @Override
    public Mono<Object> executeToMono(Command<?> command) {
        for (AbstractCommandSupport support : supports) {
            boolean supported = support.commandIsSupported0(command.getCommandId());
            if (supported) {
                return support.executeToMono(command);
            }
        }
        return Mono.error(unsupportedCommand(command));
    }

    @Override
    public Mono<Object> executeToMono(String commandId, Map<String, Object> parameters) {
        for (AbstractCommandSupport support : supports) {
            boolean supported = support.commandIsSupported0(commandId);
            if (supported) {
                return support.executeToMono(commandId, parameters);
            }
        }
        return Mono.error(unsupportedCommand(commandId));
    }

    @Override
    public <R, C extends Command<R>> C createCommand(String commandId) {
        for (AbstractCommandSupport support : supports) {
            boolean supported = support.commandIsSupported0(commandId);
            if (supported) {
                return support.createCommand(commandId);
            }
        }
        throw unsupportedCommand(commandId);
    }

    @Override
    public <R, C extends Command<R>> Mono<C> createCommandAsync(String commandId) {
        for (AbstractCommandSupport support : supports) {
            boolean supported = support.commandIsSupported0(commandId);
            if (supported) {
                return support.createCommandAsync(commandId);
            }
        }
        return Mono.error(unsupportedCommand(commandId));
    }

    @Override
    public Flux<FunctionMetadata> getCommandMetadata() {
        return Flux.fromIterable(supports)
                   .flatMap(CommandSupport::getCommandMetadata);
    }

    @Override
    public Mono<FunctionMetadata> getCommandMetadata(String commandId) {
        for (AbstractCommandSupport support : supports) {
            boolean supported = support.commandIsSupported0(commandId);
            if (supported) {
                return support.getCommandMetadata(commandId);
            }
        }
        return Mono.error(unsupportedCommand(commandId));
    }

    @Override
    public Mono<Boolean> commandIsSupported(Command<?> cmd) {
        for (AbstractCommandSupport support : supports) {
            boolean supported = support.commandIsSupported0(cmd.getCommandId());
            if (supported) {
                return Reactors.ALWAYS_TRUE;
            }
        }
        return Reactors.ALWAYS_FALSE;
    }

    @Override
    public Mono<Boolean> commandIsSupported(Class<? extends Command<?>> cmd) {
        for (AbstractCommandSupport support : supports) {
            boolean supported = support.commandIsSupported0(CommandUtils.getCommandIdByType(cmd));
            if (supported) {
                return Reactors.ALWAYS_TRUE;
            }
        }
        return Reactors.ALWAYS_FALSE;
    }

    @Override
    public Mono<Boolean> commandIsSupported(String commandId) {
        for (AbstractCommandSupport support : supports) {
            boolean supported = support.commandIsSupported0(commandId);
            if (supported) {
                return Reactors.ALWAYS_TRUE;
            }
        }
        return Reactors.ALWAYS_FALSE;
    }
}
