package org.jetlinks.core.message.codec.parser;

import io.netty.buffer.ByteBuf;

/**
 * 报文帧解析规则接口定义(新版).
 * <p>
 * 单次调用既负责匹配又负责解析, 并支持返回重同步起始下标:
 * <ul>
 *     <li>{@code notMatch()}: 当前规则在现有数据中完全不适用(找不到帧头), 不修改 readerIndex;</li>
 *     <li>{@code needMore(startIndex)}: 找到可能的帧头, 但数据不足, 建议从 {@code startIndex} 处开始等待更多数据;</li>
 *     <li>{@code success(startIndex, frame)}: 从 {@code startIndex} 解析出一帧, 实现方须已将 readerIndex 推进到帧尾之后.</li>
 * </ul>
 */
public interface MessageFrameRule {

    /**
     * 尝试从 {@code buf} 中解析一帧.
     *
     * @param buf 累积缓冲区
     * @return 解析结果
     */
    ParseResult parse(ByteBuf buf);

    final class ParseResult {
        public final int startIndex;
        public final ByteBuf frame;

        private ParseResult(int startIndex, ByteBuf frame) {
            this.startIndex = startIndex;
            this.frame = frame;
        }

        /** 当前规则在现有数据中完全不适用 */
        public static ParseResult notMatch() {
            return new ParseResult(-1, null);
        }

        /** 找到了潜在帧头, 但数据不足 */
        public static ParseResult needMore(int startIndex) {
            return new ParseResult(startIndex, null);
        }

        /** 成功从 {@code startIndex} 解析出一帧 */
        public static ParseResult success(int startIndex, ByteBuf frame) {
            return new ParseResult(startIndex, frame);
        }
    }
}

