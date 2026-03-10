package org.jetlinks.core.message.codec.parser.rule;

import io.netty.buffer.ByteBuf;
import org.jetlinks.core.message.codec.parser.MessageFrameRule;

import java.util.Objects;
import java.util.function.Predicate;

/**
 * 固定长度帧规则.
 */
public class FixedLengthFrameRule implements MessageFrameRule.FrameRule {

    /**
     * 常用 4 字节固定长度规则, 例如心跳 ping 等.
     */
    public static final FixedLengthFrameRule LENGTH_4 = new FixedLengthFrameRule(4);

    private final int length;
    private final Predicate<ByteBuf> matcher;

    public FixedLengthFrameRule(int length) {
        this(length, buf -> true);
    }

    public FixedLengthFrameRule(int length, Predicate<ByteBuf> matcher) {
        if (length <= 0) {
            throw new IllegalArgumentException("length must be > 0");
        }
        this.length = length;
        this.matcher = Objects.requireNonNull(matcher, "matcher");
    }

    @Override
    public boolean match(ByteBuf buf) {
        return buf.readableBytes() >= length && matcher.test(buf);
    }

    @Override
    public ByteBuf parse(ByteBuf buf) {
        if (buf.readableBytes() < length) {
            return null;
        }
        return buf.readRetainedSlice(length);
    }
}

