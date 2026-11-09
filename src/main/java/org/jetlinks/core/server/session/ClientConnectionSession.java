package org.jetlinks.core.server.session;

import org.jetlinks.core.server.ClientConnection;

import java.util.Collection;

public interface ClientConnectionSession {

    Collection<? extends ClientConnection> getConnections();

    /** Optional lifetime facts, reset when this session instance is replaced. */
    default org.jetlinks.core.server.ConnectionLifecycle getConnectionLifecycle() { return null; }

}
