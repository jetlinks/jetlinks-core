package org.jetlinks.core.rpc;

public interface RpcService<I> {

    String serverNodeId();

    String id();

    String name();

    I service();
}
