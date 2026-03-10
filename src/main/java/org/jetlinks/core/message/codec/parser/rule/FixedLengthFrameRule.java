package org.jetlinks.core.message.codec.parser.rule;

import io.netty.buffer.ByteBuf;
import org.jetlinks.core.message.codec.parser.MessageFrameRule;

import java.util.Objects;
import java.util.function.Predicate;

/**
 * 固定长度帧规则.
 */
public class FixedLengthFrameRule implements MessageFrameRule {

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
    public ParseResult parse(ByteBuf buf) {
        if (!matcher.test(buf)) {
            return ParseResult.notMatch();
        }
        int start = buf.readerIndex();
        int readable = buf.readableBytes();
        if (readable < length) {
            return ParseResult.needMore(start);
        }
        ByteBuf frame = buf.readRetainedSlice(length);
        return ParseResult.success(start, frame);
    }
}

