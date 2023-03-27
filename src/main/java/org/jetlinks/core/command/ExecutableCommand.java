package org.jetlinks.core.command;

public interface ExecutableCommand<Response> extends Command<Response> {

    Response execute(CommandSupport support);

}
