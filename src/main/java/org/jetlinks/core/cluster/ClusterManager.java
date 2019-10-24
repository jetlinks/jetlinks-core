package org.jetlinks.core.cluster;

public interface ClusterManager {

    String getClusterName();

    String getCurrentServerId();

    ClusterNotifier getNotifier();

    <T> ClusterQueue<T> getQueue(String queueId);

    <T> ClusterTopic<T> getTopic(String topic);

    <K, V> ClusterCache<K, V> getCache(String cache);

}
