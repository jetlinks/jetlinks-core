package org.jetlinks.core.command.blocking;

import org.jetlinks.core.command.AsyncProxyCommandSupport;
import org.jetlinks.core.command.CommandSupport;
import reactor.core.publisher.Mono;

class AsyncBlockingCommandSupport extends AsyncProxyCommandSupport implements BlockingCommandSupport {

    public AsyncBlockingCommandSupport(Mono<? extends CommandSupport> asyncCommand) {
        super(asyncCommand);
    }
}
