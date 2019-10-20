package org.jetlinks.core.server;

public interface GatewayServerContextListener<C extends GatewayServerContext> {

    void onServerUp(C context);

    void onServerDown(C context);
}
