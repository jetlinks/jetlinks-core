package org.jetlinks.core.server.session;

import org.jetlinks.core.server.ClientConnection;

import java.util.Collection;

public interface ClientConnectionSession {

    Collection<? extends ClientConnection> getConnections();

}
