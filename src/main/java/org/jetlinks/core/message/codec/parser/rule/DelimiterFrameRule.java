package org.jetlinks.core.message.codec.parser.rule;

import io.netty.buffer.ByteBuf;
import org.jetlinks.core.message.codec.parser.MessageFrameRule;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * 基于分隔符的帧规则.
 * <p>
 * 可通过 {@code excludeDelimiter} 配置解析后的报文是否包含分隔符:
 * <ul>
 *     <li>{@code false}（默认）: 帧包含分隔符;</li>
 *     <li>{@code true}: 截断分隔符, 解析后的报文不包含分隔符.</li>
 * </ul>
 */
public class DelimiterFrameRule implements MessageFrameRule.FrameRule {

    /**
     * 以 CRLF 结尾的通用规则, 帧内包含分隔符.
     */
    public static final DelimiterFrameRule CRLF =
        new DelimiterFrameRule("\r\n".getBytes(StandardCharsets.US_ASCII));

    /**
     * 以 CRLF 结尾的通用规则, 解析结果不包含分隔符.
     */
    public static final DelimiterFrameRule CRLF_EXCLUDE =
        new DelimiterFrameRule("\r\n".getBytes(StandardCharsets.US_ASCII), true);

    private final byte[] delimiter;
    private final boolean excludeDelimiter;
    private final Predicate<ByteBuf> matcher;

    public DelimiterFrameRule(byte[] delimiter) {
        this(delimiter, false, buf -> true);
    }

    public DelimiterFrameRule(byte[] delimiter, Predicate<ByteBuf> matcher) {
        this(delimiter, false, matcher);
    }

    /**
     * @param delimiter        分隔符字节序列
     * @param excludeDelimiter 为 true 时解析后的报文不包含分隔符（截断分隔符）
     */
    public DelimiterFrameRule(byte[] delimiter, boolean excludeDelimiter) {
        this(delimiter, excludeDelimiter, buf -> true);
    }

    /**
     * @param delimiter        分隔符字节序列
     * @param excludeDelimiter 为 true 时解析后的报文不包含分隔符（截断分隔符）
     * @param matcher          额外匹配条件
     */
    public DelimiterFrameRule(byte[] delimiter, boolean excludeDelimiter, Predicate<ByteBuf> matcher) {
        Objects.requireNonNull(delimiter, "delimiter");
        if (delimiter.length == 0) {
            throw new IllegalArgumentException("delimiter must not be empty");
        }
        this.delimiter = Arrays.copyOf(delimiter, delimiter.length);
        this.excludeDelimiter = excludeDelimiter;
        this.matcher = Objects.requireNonNull(matcher, "matcher");
    }

    @Override
    public boolean match(ByteBuf buf) {
        if (!matcher.test(buf)) {
            return false;
        }
        int start = buf.readerIndex();
        int end = start + buf.readableBytes() - delimiter.length + 1;
        for (int i = start; i < end; i++) {
            boolean ok = true;
            for (int j = 0; j < delimiter.length; j++) {
                if (buf.getByte(i + j) != delimiter[j]) {
                    ok = false;
                    break;
                }
            }
            if (ok) {
                return true;
            }
        }
        return false;
    }

    @Override
    public ByteBuf parse(ByteBuf buf) {
        int start = buf.readerIndex();
        int readable = buf.readableBytes();
        int end = start + readable - delimiter.length + 1;
        for (int i = start; i < end; i++) {
            boolean ok = true;
            for (int j = 0; j < delimiter.length; j++) {
                if (buf.getByte(i + j) != delimiter[j]) {
                    ok = false;
                    break;
                }
            }
            if (ok) {
                if (excludeDelimiter) {
                    int frameLength = i - start;
                    int totalNeed = frameLength + delimiter.length;
                    if (buf.readableBytes() < totalNeed) {
                        return null;
                    }
                    ByteBuf frame = buf.readRetainedSlice(frameLength);
                    buf.skipBytes(delimiter.length);
                    return frame;
                } else {
                    int frameLength = (i + delimiter.length) - start;
                    if (buf.readableBytes() < frameLength) {
                        return null;
                    }
                    return buf.readRetainedSlice(frameLength);
                }
            }
        }
        return null;
    }
}

