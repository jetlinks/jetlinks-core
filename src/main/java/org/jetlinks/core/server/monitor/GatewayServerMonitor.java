package org.jetlinks.core.server.monitor;


public interface GatewayServerMonitor {

    String getCurrentServerId();

    GatewayServerMetrics metrics();
}
