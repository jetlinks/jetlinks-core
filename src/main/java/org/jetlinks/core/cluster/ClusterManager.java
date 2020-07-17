package org.jetlinks.core.cluster;

/**
 * 集群管理器
 *
 * @author zhouhao
 * @since 1.0
 */
public interface ClusterManager {

    /**
     * @return 集群名称
     */
    String getClusterName();

    /**
     * @return 当前服务节点ID
     */
    String getCurrentServerId();

    /**
     * @return 集群通知器
     */
    ClusterNotifier getNotifier();

    /**
     * @return 高可用管理器
     */
    HaManager getHaManager();

    /**
     * 获取集群队列
     *
     * @param queueId 队列标识
     * @param <T>     队列中元素类型
     * @return 集群队列
     */
    <T> ClusterQueue<T> getQueue(String queueId);

    /**
     * 获取集群广播
     *
     * @param topic 广播标识
     * @param <T>   数据类型
     * @return 集群广播
     */
    <T> ClusterTopic<T> getTopic(String topic);

    /**
     * 获取集群缓存
     *
     * @param cache 缓存标识
     * @param <K>   Key类型
     * @param <V>   Value类型
     * @return 集群缓存
     */
    <K, V> ClusterCache<K, V> getCache(String cache);

    /**
     * 获取Set结构
     * @param name 名称
     * @param <V> 集合元素类型
     * @return ClusterSet
     */
    <V> ClusterSet<V> getSet(String name);

    /**
     * 获取计数器
     * @param name 名称
     * @return 计数器
     */
    ClusterCounter getCounter(String name);
}
