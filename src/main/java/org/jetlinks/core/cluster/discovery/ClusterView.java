package org.jetlinks.core.cluster.discovery;
import java.util.Map;
/** provider 本地单调版本的不可变成员视图，不表示全局共识。 */
public record ClusterView(long revision, Map<NodeIdentity, NodeAnnouncement> members) {
    public ClusterView { members = Map.copyOf(members); }
}
