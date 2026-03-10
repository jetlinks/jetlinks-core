package org.jetlinks.core.message.codec.parser.rule;

import io.netty.buffer.ByteBuf;
import org.jetlinks.core.message.codec.parser.MessageFrameRule;

import java.util.Objects;
import java.util.function.Predicate;

/**
 * 基于长度字段的帧规则.
 * <p>
 * 约定:
 * <ul>
 *     <li>长度字段位置从帧起始 readerIndex 偏移 {@code lengthFieldOffset};</li>
 *     <li>长度字段值为负载(body)长度, 使用大端字节序;</li>
 *     <li>可选尾长 {@code tailLength}, 用于 [header][body_len][body][crc] 等场景;</li>
 *     <li>帧总长度为 {@code headerLength + lengthFieldValue + tailLength}.</li>
 * </ul>
 * <p>
 * 示例: header=4(含 body_len 2 字节), body 长度在偏移 2 处 2 字节, 尾 CRC 2 字节:
 * <pre>new LengthFieldFrameRule(2, 2, 4, 2)</pre>
 */
public class LengthFieldFrameRule implements MessageFrameRule.FrameRule {

    private final int lengthFieldOffset;
    private final int lengthFieldLength;
    private final int headerLength;
    private final int tailLength;
    private final Predicate<ByteBuf> matcher;

    /**
     * 常见「2 字节长度字段 + 4 字节头部」的通用规则, 无尾部.
     */
    public static final LengthFieldFrameRule HEADER4_LEN2 =
        new LengthFieldFrameRule(2, 2, 4);

    /**
     * 常见「2 字节长度字段 + 4 字节头部 + 2 字节 CRC 尾」的通用规则.
     */
    public static final LengthFieldFrameRule HEADER4_LEN2_TAIL2 =
        new LengthFieldFrameRule(2, 2, 4, 2);

    public LengthFieldFrameRule(int lengthFieldOffset,
                                int lengthFieldLength,
                                int headerLength) {
        this(lengthFieldOffset, lengthFieldLength, headerLength, 0, buf -> true);
    }

    public LengthFieldFrameRule(int lengthFieldOffset,
                                int lengthFieldLength,
                                int headerLength,
                                Predicate<ByteBuf> matcher) {
        this(lengthFieldOffset, lengthFieldLength, headerLength, 0, matcher);
    }

    /**
     * @param lengthFieldOffset 长度字段相对帧起始的偏移
     * @param lengthFieldLength 长度字段字节数 (1/2/4)
     * @param headerLength      头部长度 (含长度字段, 即 body 起始前的字节数)
     * @param tailLength        尾部长度 (如 CRC 2 字节), 0 表示无尾
     */
    public LengthFieldFrameRule(int lengthFieldOffset,
                                int lengthFieldLength,
                                int headerLength,
                                int tailLength) {
        this(lengthFieldOffset, lengthFieldLength, headerLength, tailLength, buf -> true);
    }

    /**
     * @param lengthFieldOffset 长度字段相对帧起始的偏移
     * @param lengthFieldLength 长度字段字节数 (1/2/4)
     * @param headerLength      头部长度 (含长度字段)
     * @param tailLength        尾部长度 (如 CRC), 0 表示无尾
     * @param matcher           额外匹配条件
     */
    public LengthFieldFrameRule(int lengthFieldOffset,
                                int lengthFieldLength,
                                int headerLength,
                                int tailLength,
                                Predicate<ByteBuf> matcher) {
        if (lengthFieldLength != 1 && lengthFieldLength != 2 && lengthFieldLength != 4) {
            throw new IllegalArgumentException("lengthFieldLength must be 1,2 or 4");
        }
        if (tailLength < 0) {
            throw new IllegalArgumentException("tailLength must be >= 0");
        }
        this.lengthFieldOffset = lengthFieldOffset;
        this.lengthFieldLength = lengthFieldLength;
        this.headerLength = headerLength;
        this.tailLength = tailLength;
        this.matcher = Objects.requireNonNull(matcher, "matcher");
    }

    @Override
    public boolean match(ByteBuf buf) {
        if (!matcher.test(buf)) {
            return false;
        }
        int readable = buf.readableBytes();
        return readable >= headerLength
            && readable >= (lengthFieldOffset + lengthFieldLength)
            && readable > 0;
    }

    @Override
    public ByteBuf parse(ByteBuf buf) {
        int start = buf.readerIndex();
        int readable = buf.readableBytes();
        if (readable < (lengthFieldOffset + lengthFieldLength)) {
            return null;
        }

        int fieldIndex = start + lengthFieldOffset;
        int len = switch (lengthFieldLength) {
            case 1 -> buf.getUnsignedByte(fieldIndex);
            case 2 -> buf.getUnsignedShort(fieldIndex);
            case 4 -> buf.getInt(fieldIndex);
            default -> throw new IllegalStateException("Unsupported lengthFieldLength: " + lengthFieldLength);
        };

        int frameLength = headerLength + len + tailLength;
        if (frameLength < 0) {
            throw new IllegalStateException("Negative frame length: " + frameLength);
        }
        if (buf.readableBytes() < frameLength) {
            return null;
        }
        return buf.readRetainedSlice(frameLength);
    }
}

