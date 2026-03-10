package org.jetlinks.core.message.codec.parser.rule;

import io.netty.buffer.ByteBuf;
import org.jetlinks.core.message.codec.parser.MessageFrameRule;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * 以指定字节序列「开头」和「结尾」的帧解析规则.
 * <p>
 * 帧格式: {@code start + 中间任意内容 + end}, 帧内包含 start 与 end 本身.
 * 从当前 readerIndex 起必须与 start 匹配, 并在其后首次出现 end 时截断为一帧.
 */
public class StartEndFrameRule implements MessageFrameRule.FrameRule {

    private final byte[] start;
    private final byte[] end;
    private final Predicate<ByteBuf> matcher;

    /**
     * @param start 帧开头字节序列
     * @param end   帧结尾字节序列
     */
    public StartEndFrameRule(byte[] start, byte[] end) {
        this(start, end, buf -> true);
    }

    /**
     * @param start  帧开头字节序列
     * @param end    帧结尾字节序列
     * @param matcher 额外匹配条件, 不应修改 readerIndex
     */
    public StartEndFrameRule(byte[] start, byte[] end, Predicate<ByteBuf> matcher) {
        Objects.requireNonNull(start, "start");
        Objects.requireNonNull(end, "end");
        if (start.length == 0) {
            throw new IllegalArgumentException("start must not be empty");
        }
        if (end.length == 0) {
            throw new IllegalArgumentException("end must not be empty");
        }
        this.start = Arrays.copyOf(start, start.length);
        this.end = Arrays.copyOf(end, end.length);
        this.matcher = Objects.requireNonNull(matcher, "matcher");
    }

    @Override
    public boolean match(ByteBuf buf) {
        if (!matcher.test(buf)) {
            return false;
        }
        int readable = buf.readableBytes();
        if (readable < start.length + end.length) {
            return false;
        }
        int index = buf.readerIndex();
        for (int i = 0; i < start.length; i++) {
            if (buf.getByte(index + i) != start[i]) {
                return false;
            }
        }
        int searchFrom = index + start.length;
        int searchTo = index + readable - end.length + 1;
        for (int i = searchFrom; i < searchTo; i++) {
            boolean found = true;
            for (int j = 0; j < end.length; j++) {
                if (buf.getByte(i + j) != end[j]) {
                    found = false;
                    break;
                }
            }
            if (found) {
                return true;
            }
        }
        return false;
    }

    @Override
    public ByteBuf parse(ByteBuf buf) {
        int index = buf.readerIndex();
        int readable = buf.readableBytes();
        if (readable < start.length + end.length) {
            return null;
        }
        for (int i = 0; i < start.length; i++) {
            if (buf.getByte(index + i) != start[i]) {
                return null;
            }
        }
        int searchFrom = index + start.length;
        int searchTo = index + readable - end.length + 1;
        for (int i = searchFrom; i < searchTo; i++) {
            boolean found = true;
            for (int j = 0; j < end.length; j++) {
                if (buf.getByte(i + j) != end[j]) {
                    found = false;
                    break;
                }
            }
            if (found) {
                int frameLength = (i + end.length) - index;
                if (buf.readableBytes() < frameLength) {
                    return null;
                }
                return buf.readRetainedSlice(frameLength);
            }
        }
        return null;
    }
}
