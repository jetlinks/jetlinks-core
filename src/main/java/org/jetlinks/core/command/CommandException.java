package org.jetlinks.core.command;

import lombok.Getter;
import org.hswebframework.web.exception.I18nSupportException;

@Getter
public class CommandException extends I18nSupportException {
    private final CommandSupport support;
    private final Command<?> command;

    public CommandException(CommandSupport support,
                            Command<?> command,
                            String code) {
        super(code);
        this.support = support;
        this.command = command;
    }

    public CommandException(CommandSupport support,
                            Command<?> command,
                            String code,
                            Throwable cause,
                            Object... args) {
        super(code, cause, args);
        this.support = support;
        this.command = command;
    }
}
