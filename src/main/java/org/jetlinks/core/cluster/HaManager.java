package org.jetlinks.core.cluster;

import reactor.core.publisher.Flux;

import java.util.List;

public interface HaManager {

    ServerNode currentServer();

    Flux<ServerNode> subscribeServerOnline();

    Flux<ServerNode> subscribeServerOffline();

    List<ServerNode> getAllNode();

}
