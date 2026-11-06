package org.jetlinks.core.metadata;

import java.util.Map;

/**
 * 扩展定义方通过 SPI 声明默认合并规则，由 {@link ExpandsMerges} 在首次使用时发现。
 * 规则只描述扩展键的含义，不持有请求状态；同一 SPI 键只能由一个提供方声明。
 * 应用需覆盖规则时使用显式注册，以获得可释放的生命周期。
 *
 * @since 1.3.3
 * @see ExpandsMerges#register(Map)
 */
public interface ExpandsMergeProvider {

    /**
     * 初始化默认策略时同步调用一次。实现不得注册其他策略或执行阻塞 I/O。
     * 返回值会被复制，空 map 表示无贡献；null 或重复的 SPI 键属于装配错误。
     * 合并回调应可并发复用，不修改输入值；异常直接传播，不自动重试或降级。
     *
     * @return 顶层扩展键及其完整规则；对象子键使用 {@link ExpandsMerge#object(Map)} 声明
     * @since 1.3.3
     */
    Map<String, ExpandsMerge> getRules();
}
