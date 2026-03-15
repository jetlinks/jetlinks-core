package org.jetlinks.core.message.codec.parser.rule;

import io.netty.buffer.ByteBuf;
import org.jetlinks.core.message.codec.parser.MessageFrameRule;

/**
 * 通用 Modbus RTU 帧解析规则.
 * <p>
 * 自动根据功能码和报文结构计算帧长度, 支持常见请求/响应以及异常响应的分包解析, 无需额外配置长度或 matcher:
 * <ul>
 *     <li>请求: 0x01,0x02,0x03,0x04,0x05,0x06,0x0F,0x10;</li>
 *     <li>正常响应: 0x01,0x02,0x03,0x04(带 Byte Count 字段), 0x05,0x06,0x0F,0x10(固定长度回显);</li>
 *     <li>异常响应: 功能码最高位为 1 ({@code function | 0x80}), 长度固定为 5 字节.</li>
 *     <li>功能码 0x20: 厂商/扩展读响应，固定 35 字节（与 0x03 类 35 字节响应同结构），需 CRC 校验.</li>
 * </ul>
 *
 * <p>帧最小长度为 5 字节: 地址(1) + 功能码(1) + 数据至少 1 字节 + CRC(2).</p>
 *
 * <p>提供通用静态实例 {@link #INSTANCE}, 避免在常见场景中重复创建规则对象.</p>
 */
public class ModbusRtuFrameRule implements MessageFrameRule {

    /** Modbus 读响应 ByteCount 上限 (协议最大 250) */
    private static final int MAX_READ_RESPONSE_BYTE_COUNT = 250;

    /** 01~04 响应“还差字节数”超过此值时不再 needMore，改为 notMatch，避免脏数据导致长期阻塞 */
    private static final int MAX_SHORTFALL_FOR_NEED_MORE = 128;

    /**
     * 通用 Modbus RTU 规则单例.
     */
    public static final ModbusRtuFrameRule INSTANCE = new ModbusRtuFrameRule();

    @Override
    public ParseResult parse(ByteBuf buf) {
        int index = buf.readerIndex();
        int readable = buf.readableBytes();
        if (readable < 5) {
            return ParseResult.needMore(index);
        }

        int function = buf.getUnsignedByte(index + 1);

        // 异常响应: 地址 + (功能码|0x80) + 异常码 + CRC(2) = 5 字节
        if ((function & 0x80) != 0) {
            int frameLength = 5;
            // 数据已经足够 5 字节，此时必须通过 CRC 校验才认为是合法异常响应
            if (isCrcValid(buf, index, frameLength)) {
                buf.readerIndex(index + frameLength);
                ByteBuf frame = buf.slice(index, frameLength).retain();
                return ParseResult.success(index, frame);
            }
            // CRC 不通过，当前规则不适用，交给后续规则或上层处理(例如丢弃字节)
            return ParseResult.notMatch();
        }

        int baseFunc = function & 0x7F;

        switch (baseFunc) {
            // ======================= 01~04: 区分请求和响应 =======================
            case 1:
            case 2:
            case 3:
            case 4: {
                // Modbus RTU: 01~04 功能码既有 8 字节请求，也有带 ByteCount 的响应。
                // 这里通过 CRC 校验来判断当前数据更像是请求还是响应，避免跨帧误拆。

                // 请求固定长度 8: 地址(1)+功能码(1)+起始地址(2)+数量(2)+CRC(2)
                boolean requestOk = readable >= 8 && isCrcValid(buf, index, 8);

                // 响应: 地址(1)+功能码(1)+ByteCount(1)+数据(N)+CRC(2)，总长度 = 3+ByteCount+2 = 5+ByteCount
                boolean responseOk = false;
                int respLen = -1;
                {
                    int byteCount = buf.getUnsignedByte(index + 2);
                    // byteCount 为 0 或超过协议上限时均不按响应解析，避免脏数据导致长期 needMore
                    if (byteCount > 0 && byteCount <= MAX_READ_RESPONSE_BYTE_COUNT) {
                        respLen = 5 + byteCount;
                        if (readable >= respLen && isCrcValid(buf, index, respLen)) {
                            responseOk = true;
                        }
                    }
                }

                if (responseOk && !requestOk) {
                    // 只命中响应分支
                    buf.readerIndex(index + respLen);
                    ByteBuf frame = buf.slice(index, respLen).retain();
                    return ParseResult.success(index, frame);
                }

                if (requestOk && !responseOk) {
                    // 只命中请求分支
                    buf.readerIndex(index + 8);
                    ByteBuf frame = buf.slice(index, 8).retain();
                    return ParseResult.success(index, frame);
                }

                if (requestOk) {
                    // 同时通过 CRC 的情况极少，一般认为响应更长、信息更多，优先按响应处理。
                    buf.readerIndex(index + respLen);
                    ByteBuf frame = buf.slice(index, respLen).retain();
                    return ParseResult.success(index, frame);
                }

                // 都无法通过 CRC 校验:
                // - 如果缓冲区还不够长，等待更多数据；
                // - 若“还差字节数”过大则不再 needMore，改为 notMatch，避免错误 byteCount 导致长期阻塞、后续报文无法解析。
                if (readable < 8) {
                    int byteCount = buf.getUnsignedByte(index + 2);
                    if (byteCount > 0 && byteCount <= MAX_READ_RESPONSE_BYTE_COUNT) {
                        int need = (5 + byteCount) - readable;
                        if (need > MAX_SHORTFALL_FOR_NEED_MORE) {
                            return ParseResult.notMatch();
                        }
                    }
                    return ParseResult.needMore(index);
                }
                return ParseResult.notMatch();
            }

            // ======================= 05,06: 固定 8 字节 =======================
            case 5:
            case 6: {
                int frameLength = 8;
                if (readable < frameLength) {
                    return ParseResult.needMore(index);
                }
                if (!isCrcValid(buf, index, frameLength)) {
                    // 即使长度满足，如果 CRC 不通过也认为不是合法 Modbus 帧
                    return ParseResult.notMatch();
                }
                buf.readerIndex(index + frameLength);
                ByteBuf frame = buf.slice(index, frameLength).retain();
                return ParseResult.success(index, frame);
            }

            // ======================= 0F,10: 写多个 =======================
            case 15:
            case 16: {
                // 响应固定 8 字节: 地址(1)+功能码(1)+起始(2)+数量(2)+CRC(2)
                if (readable == 8) {
                    if (!isCrcValid(buf, index, 8)) {
                        return ParseResult.notMatch();
                    }
                    buf.readerIndex(index + 8);
                    ByteBuf frame = buf.slice(index, 8).retain();
                    return ParseResult.success(index, frame);
                }

                // 请求: 地址(1)+功能码(1)+起始(2)+数量(2)+字节数(1)+数据(N)+CRC(2)
                // 最小长度: 9，且 byteCount 必须 > 0（0x0F 至少 1 字节线圈数据，0x10 至少 2 字节寄存器数据），
                // 否则会误把无效帧尾部（如 d2 0f 0b ce 00 00 00 00 00）当成合法 9 字节请求并消费，导致“只丢到 60”而整段未丢。
                if (readable >= 9) {
                    int byteCount = buf.getUnsignedByte(index + 6);
                    if (byteCount > 0) {
                        int reqLen = 6 + 1 + byteCount + 2;
                        if (readable >= reqLen && isCrcValid(buf, index, reqLen)) {
                            buf.readerIndex(index + reqLen);
                            ByteBuf frame = buf.slice(index, reqLen).retain();
                            return ParseResult.success(index, frame);
                        }
                    } else {
                        // byteCount==0 的 9 字节不是合法请求，返回 notMatch 让上层逐字节丢弃，避免 needMore 导致卡住不丢
                        return ParseResult.notMatch();
                    }
                }
                return ParseResult.needMore(index);
            }

            // ======================= 0x20: 厂商/扩展读响应，固定 35 字节 =======================
            case 32: {
                int frameLength = 35;
                if (readable < frameLength) {
                    return ParseResult.needMore(index);
                }
                if (!isCrcValid(buf, index, frameLength)) {
                    return ParseResult.notMatch();
                }
                buf.readerIndex(index + frameLength);
                ByteBuf frame = buf.slice(index, frameLength).retain();
                return ParseResult.success(index, frame);
            }

            default:
                return ParseResult.notMatch();
        }
    }

    /**
     * 校验从 {@code index} 开始、总长度为 {@code frameLength} 字节的数据是否满足 Modbus RTU CRC16 校验。
     *
     * @param buf         缓冲区
     * @param index       起始下标
     * @param frameLength 帧总长度(含 CRC2 字节)
     * @return CRC 是否正确
     */
    private static boolean isCrcValid(ByteBuf buf, int index, int frameLength) {
        if (frameLength < 5) {
            return false;
        }
        int dataLength = frameLength - 2;
        // 使用绝对下标：需保证 [index, index+frameLength) 均在可读范围内
        int readerIndex = buf.readerIndex();
        int readable = buf.readableBytes();
        if (index + frameLength > readerIndex + readable) {
            return false;
        }
        // Modbus RTU 规定 CRC 在线上为低字节在前，即 LE
        int expectedCrc = buf.getUnsignedShortLE(index + dataLength);
        int actualCrc = calcModbusCrc(buf, index, dataLength);
        return expectedCrc == actualCrc;
    }

    /**
     * 计算 Modbus RTU CRC16，标准多项式 0xA001，初始值 0xFFFF，低字节在前。
     */
    private static int calcModbusCrc(ByteBuf buf, int index, int length) {
        int crc = 0xFFFF;
        for (int i = 0; i < length; i++) {
            crc ^= buf.getUnsignedByte(index + i);
            for (int j = 0; j < 8; j++) {
                if ((crc & 0x0001) != 0) {
                    crc = (crc >>> 1) ^ 0xA001;
                } else {
                    crc = (crc >>> 1);
                }
            }
        }
        return crc & 0xFFFF;
    }
}

