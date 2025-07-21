package org.jetlinks.core.command.blocking;

import lombok.AllArgsConstructor;
import org.jetlinks.core.command.CommandSupport;
import org.jetlinks.core.command.ProxyCommandSupport;

@AllArgsConstructor
class DefaultBlockingCommandSupport implements BlockingCommandSupport, ProxyCommandSupport {

    protected final CommandSupport target;

    @Override
    public CommandSupport getProxyTarget() {
        return target;
    }


}
