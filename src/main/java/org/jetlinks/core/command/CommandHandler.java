package org.jetlinks.core.command;

import org.jetlinks.core.metadata.FunctionMetadata;

import javax.annotation.Nonnull;
import java.util.function.BiFunction;
import java.util.function.Supplier;

public interface CommandHandler<C extends Command<Response>, Response> {

    Response handle(@Nonnull C command, @Nonnull CommandSupport support);

    @Nonnull
    C createCommand();

    FunctionMetadata getMetadata();

    static <R, C extends Command<R>> CommandHandler<C, R> of(FunctionMetadata metadata,
                                                             BiFunction<C, CommandSupport, R> executor,
                                                             Supplier<C> commandBuilder) {
        return of(() -> metadata, executor, commandBuilder);
    }

    static <R, C extends Command<R>> CommandHandler<C, R> of(Supplier<FunctionMetadata> metadata,
                                                             BiFunction<C, CommandSupport, R> executor,
                                                             Supplier<C> commandBuilder) {
        return new SimpleCommandHandler<>(metadata, executor, commandBuilder);
    }

}
