package org.jetlinks.core.command;

import org.jetlinks.core.Wrapper;

import javax.annotation.Nonnull;

public interface CommandSupport extends Wrapper {

    @Nonnull
    <R> R execute(@Nonnull Command<R> command);

}
