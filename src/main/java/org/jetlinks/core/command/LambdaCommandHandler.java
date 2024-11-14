package org.jetlinks.core.command;

import lombok.AllArgsConstructor;
import org.jetlinks.core.metadata.FunctionMetadata;

import javax.annotation.Nonnull;
import java.util.function.BiFunction;
import java.util.function.Supplier;

@AllArgsConstructor
class LambdaCommandHandler<C extends Command<R>, R> implements CommandHandler<C, R> {
    private final Supplier<FunctionMetadata> description;

    private final BiFunction<C, CommandSupport, R> handler;

    private final Supplier<C> commandBuilder;

    @Override
    public R handle(@Nonnull C command, @Nonnull CommandSupport support) {
        return handler.apply(command, support);
    }

    @Nonnull
    @Override
    public C createCommand() {
        return commandBuilder.get();
    }

    @Override
    public FunctionMetadata getMetadata() {
        return description.get();
    }


    @Override
    public String toString() {
        return String.valueOf(description.get());
    }
}
