package org.jetlinks.core.topic;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * {@link TopicSubscriptionPlan} 的版本化二进制编解码 SPI。
 * <p>
 * codec 只处理 Route Plan，不处理 subscriber、features、revision 或上层 Envelope。
 * 具体 wire format 不属于 core；实现必须在分配集合或字符串前应用调用方 limits。
 *
 * @see TopicSubscriptionPlan
 * @see TopicSubscriptionPlanDecodeLimits
 * @since 1.2.6
 */
public interface TopicSubscriptionPlanCodec {

    /**
     * @return 此 codec 支持的正整数格式版本
     * @since 1.2.6
     */
    int version();

    /**
     * 以确定性顺序编码完整 Route Plan。
     * <p>
     * 未知 Route 类型必须显式失败，不能降级为 wildcard。该方法不关闭或刷新 output。
     *
     * @param plan 待编码 Plan，不能为 {@code null}
     * @param output 目标输出，不能为 {@code null}，生命周期由调用方管理
     * @throws IOException 写入失败或 Route 类型不受支持
     * @since 1.2.6
     */
    void encode(TopicSubscriptionPlan plan, DataOutput output) throws IOException;

    /**
     * 解码并校验完整 Route Plan。
     * <p>
     * 调用方必须先根据外部 frame 元数据选择对应版本的 codec，并把 input 限定在当前
     * payload 的精确边界内。实现必须先校验 payloadLength，再分配任何集合或字符串；
     * 未知 Route、截断、非法长度和超过 limits 均以 IOException 显式失败。本方法不
     * 关闭 input。
     *
     * @param input 当前 payload 的有界输入，不能为 {@code null}
     * @param payloadLength 当前 payload 字节长度，不能为负数
     * @param limits 本次解码的强制资源限制，不能为 {@code null}
     * @return 完整不可变 Plan，不会返回 {@code null}
     * @throws IOException 输入非法、截断、不支持或超过限制
     * @since 1.2.6
     */
    TopicSubscriptionPlan decode(DataInput input,
                                 int payloadLength,
                                 TopicSubscriptionPlanDecodeLimits limits) throws IOException;
}
