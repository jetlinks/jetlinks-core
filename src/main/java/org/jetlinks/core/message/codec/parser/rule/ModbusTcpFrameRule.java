package org.jetlinks.core.message.codec.parser.rule;

import io.netty.buffer.ByteBuf;
import org.jetlinks.core.message.codec.parser.MessageFrameRule;

import java.util.Objects;
import java.util.function.Predicate;

/**
 * Modbus TCP 帧解析规则.
 * <p>
 * 标准 Modbus TCP 报文头结构:
 * <pre>
 * Transaction Id : 2 bytes
 * Protocol Id    : 2 bytes (通常为 0)
 * Length         : 2 bytes (Unit Id + PDU 长度)
 * Unit Id        : 1 byte
 * </pre>
 * 因此帧总长度 = 6(事务+协议+长度) + Length 字段值.
 *
 * <p>提供默认协议号为 0 的静态实例 {@link #DEFAULT}, 避免重复创建规则对象.</p>
 */
public class ModbusTcpFrameRule implements MessageFrameRule {

    private final boolean checkProtocolId;
    private final int expectedProtocolId;
    private final Predicate<ByteBuf> matcher;

    /**
     * 默认协议号 0 的通用规则实例.
     */
    public static final ModbusTcpFrameRule DEFAULT = new ModbusTcpFrameRule();

    /**
     * 使用默认协议号 0 的规则.
     */
    public ModbusTcpFrameRule() {
        this(true, 0, buf -> true);
    }

    /**
     * @param checkProtocolId   是否校验协议号
     * @param expectedProtocolId 期望的协议号, 通常为 0
     * @param matcher           额外匹配条件, 不应修改 readerIndex
     */
    public ModbusTcpFrameRule(boolean checkProtocolId,
                              int expectedProtocolId,
                              Predicate<ByteBuf> matcher) {
        this.checkProtocolId = checkProtocolId;
        this.expectedProtocolId = expectedProtocolId;
        this.matcher = Objects.requireNonNull(matcher, "matcher");
    }

    @Override
    public ParseResult parse(ByteBuf buf) {
        int readable = buf.readableBytes();
        int index = buf.readerIndex();
        if (readable < 7) {
            return ParseResult.needMore(index);
        }
        if (checkProtocolId) {
            int proto = buf.getUnsignedShort(index + 2);
            if (proto != expectedProtocolId) {
                return ParseResult.notMatch();
            }
        }
        if (!matcher.test(buf)) {
            return ParseResult.notMatch();
        }

        int len = buf.getUnsignedShort(index + 4);
        int frameLength = 6 + len;
        if (frameLength <= 0) {
            throw new IllegalStateException("Illegal Modbus TCP frame length: " + frameLength);
        }
        if (readable < frameLength) {
            return ParseResult.needMore(index);
        }
        buf.readerIndex(index + frameLength);
        ByteBuf frame = buf.slice(index, frameLength).retain();
        return ParseResult.success(index, frame);
    }
}

