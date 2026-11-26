package org.jetlinks.core.rpc;

/** RPC 统一入口；服务注册与发现能力由 {@link RpcServiceRegistry} 定义。 */
public interface RpcManager extends RpcServiceRegistry {
    String currentServerId();

    default boolean isShutdown() {
        return false;
    }
}
