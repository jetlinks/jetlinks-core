package org.jetlinks.core;

import org.jetlinks.core.utils.HashUtils;

/**
 * 实现此接口,标记对象在跨节点传递时使用自定义路由key进行路由.
 *
 * @author zhouhao
 * @since 1.2.3
 */
public interface Routable {

    /**
     * 共享对象的唯一标识,通常用于在负载均衡时,将相同的key的对象分配到同一个节点
     *
     * @return key
     * @see org.jetlinks.core.event.EventBus
     * @see org.jetlinks.core.event.Subscription.Feature#shared
     */
    Object routeKey();

    /**
     * 计算对象的hash值
     *
     * @param objects 对象
     * @return hash
     */
    @SuppressWarnings("all")
    default long hash(Object... objects) {
        return HashUtils.murmur3_128(routeKey(), objects);
    }
}
