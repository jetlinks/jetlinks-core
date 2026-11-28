package org.jetlinks.core.cluster.discovery;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
/** 成员发现 SPI，不负责 RPC 目录、选主或复制；一个实例只启动一次。 */
public interface NodeDiscovery {
    Mono<Void> start(NodeAnnouncement local);
    /** 本地非阻塞读取。 */
    ClusterView currentView();
    /** 原子交付当前完整视图及更新；慢观察者允许合并中间视图，缓冲必须有界。 */
    Flux<ClusterView> watch();
    /** 幂等关闭自己的注册/资源，不关闭外部借用的集群实例。 */
    Mono<Void> stop();
}
