package org.jetlinks.core.cluster;

import reactor.core.publisher.Flux;

import java.util.List;

/**
 * 集群高可用管理器,可用于监听集群中的节点上下线信息.
 *
 * @author zhouhao
 * @since 1.0
 */
public interface HaManager {

    /**
     * @return 当前服务节点信息
     */
    ServerNode currentServer();

    /**
     * @return 订阅服务节点上线信息
     */
    Flux<ServerNode> subscribeServerOnline();

    /**
     * @return 订阅服务节点下线信息
     */
    Flux<ServerNode> subscribeServerOffline();

    /**
     * @return 所有可用服务节点
     */
    List<ServerNode> getAllNode();

}
