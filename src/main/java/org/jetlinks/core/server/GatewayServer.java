package org.jetlinks.core.server;

import org.jetlinks.core.message.codec.Transport;

public interface GatewayServer  {

    Transport getTransport();
}
