package org.jetlinks.core.rpc;
import org.jetlinks.core.cluster.discovery.NodeIdentity;
import java.util.List;
import java.util.Set;
/** 单一本地目录版本；unresolved 是成员已知但服务目录尚未确认的实例。 */
public record RpcServiceView<I>(long revision, List<RpcService<I>> services, Set<NodeIdentity> unresolved) {
    public RpcServiceView {
        services = List.copyOf(services);
        unresolved = Set.copyOf(unresolved);
    }
}
