package org.jetlinks.core.cluster.discovery;
import java.net.URI;
import java.util.Map;
import java.util.Objects;
/** 小型节点公告，不承载服务目录、业务数据或用户身份。 */
public record NodeAnnouncement(NodeIdentity identity, String namespace, String application,
                               Map<String, String> labels, Map<String, URI> endpoints) {
    public NodeAnnouncement {
        Objects.requireNonNull(identity, "identity");
        Objects.requireNonNull(namespace, "namespace");
        Objects.requireNonNull(application, "application");
        labels = Map.copyOf(labels);
        endpoints = Map.copyOf(endpoints);
    }
}
