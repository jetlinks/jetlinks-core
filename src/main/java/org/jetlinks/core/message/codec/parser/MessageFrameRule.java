package org.jetlinks.core.message.codec.parser;

import io.netty.buffer.ByteBuf;

/**
 * 报文帧解析规则接口定义.
 *
 * <p>仅包含通用接口, 具体规则实现在 {@code org.jetlinks.core.message.codec.parser.rule} 包中.</p>
 */
public final class MessageFrameRule {

    private MessageFrameRule() {
    }

    /**
     * 单条帧解析规则.
     * <p>
     * 约定:
     * <ul>
     *     <li>{@link #match(ByteBuf)} 不应改变 {@code readerIndex};</li>
     *     <li>{@link #parse(ByteBuf)} 在返回 {@code null} 表示数据不足时, 不应改变 {@code readerIndex};</li>
     *     <li>{@link #parse(ByteBuf)} 在成功返回帧时, 应正确推进 {@code readerIndex}.</li>
     * </ul>
     */
    public interface FrameRule {

        /**
         * 判断当前缓冲区是否符合该规则.
         *
         * @param buf 累积缓冲区
         * @return 是否匹配
         */
        boolean match(ByteBuf buf);

        /**
         * 按当前规则从缓冲区中解析一帧数据.
         *
         * @param buf 累积缓冲区
         * @return 完整帧, 若返回 {@code null} 表示当前数据不足以构成一帧
         */
        ByteBuf parse(ByteBuf buf);
    }

}

