package org.jetlinks.core.cluster.discovery;
import java.util.Objects;
import java.util.UUID;
/** 逻辑节点名与本次运行身份；旧实例事件不得撤销同名新实例。 */
public record NodeIdentity(String nodeId, UUID incarnation) {
    public NodeIdentity {
        Objects.requireNonNull(nodeId, "nodeId");
        Objects.requireNonNull(incarnation, "incarnation");
    }
}
