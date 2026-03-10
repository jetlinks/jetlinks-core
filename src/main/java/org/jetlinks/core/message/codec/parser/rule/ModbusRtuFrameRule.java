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
 * </ul>
 *
 * <p>帧最小长度为 5 字节: 地址(1) + 功能码(1) + 数据至少 1 字节 + CRC(2).</p>
 *
 * <p>提供通用静态实例 {@link #INSTANCE}, 避免在常见场景中重复创建规则对象.</p>
 */
public class ModbusRtuFrameRule implements MessageFrameRule.FrameRule {

    /**
     * 通用 Modbus RTU 规则单例.
     */
    public static final ModbusRtuFrameRule INSTANCE = new ModbusRtuFrameRule();

    @Override
    public boolean match(ByteBuf buf) {
        // 至少要有 地址 + 功能码 + CRC = 4, 这里要求 5 以兼容绝大多数场景
        if (buf.readableBytes() < 5) {
            return false;
        }
        // 这里只检查功能码范围是否在常见 Modbus 功能码内, 详细长度判断在 parse 中完成
        int index = buf.readerIndex();
        int function = buf.getUnsignedByte(index + 1);
        int baseFunc = function & 0x7F;
        return switch (baseFunc) {  // Read Coils
            // Read Discrete Inputs
            // Read Holding Registers
            // Read Input Registers
            // Write Single Coil
            // Write Single Register
            // Write Multiple Coils
            case 1, 2, 3, 4, 5, 6, 15, 16 -> {
                // 增加基本校验: 如果是 Modbus TCP (协议号为 0), 且长度足够, 则 RTU 不应抢占.
                // 注意: Modbus RTU 头部如果是 [UnitId, Func, 0x00, 0x00], 可能会被误判为 TCP.
                // 真正的 Modbus TCP 头部 6 字节后通常跟 UnitId, Func.
                // 这里我们简单检查: 如果满足协议号 0 且长度 >= 8,
                // 我们再看 index + 7 (TCP 的 Func) 是否也是有效功能码.
                if (buf.readableBytes() >= 8) {
                    int proto = buf.getUnsignedShort(index + 2);
                    if (proto == 0) {
                        int tcpFunc = buf.getUnsignedByte(index + 7);
                        if ((tcpFunc & 0x7F) == 3 || (tcpFunc & 0x7F) == 1 || (tcpFunc & 0x7F) == 2 || (tcpFunc & 0x7F) == 4 || (tcpFunc & 0x7F) == 5 || (tcpFunc & 0x7F) == 6 || (tcpFunc & 0x7F) == 15 || (tcpFunc & 0x7F) == 16) {
                            yield false;
                        }
                    }
                }
                yield true;
            }
            default ->
                // 其他功能码暂不处理
                false;
        };
    }

    @Override
    public ByteBuf parse(ByteBuf buf) {
        if (buf.readableBytes() < 5) {
            return null;
        }

        int index = buf.readerIndex();
        int function = buf.getUnsignedByte(index + 1);

        // 异常响应: 地址 + (功能码|0x80) + 异常码 + CRC(2) = 5 字节
        if ((function & 0x80) != 0) {
            int frameLength = 5;
            if (buf.readableBytes() < frameLength) {
                return null;
            }
            return buf.readRetainedSlice(frameLength);
        }

        int baseFunc = function & 0x7F;

        switch (baseFunc) {
            // ======================= 01~04: 区分请求和响应 =======================
            case 1:
            case 2:
            case 3:
            case 4: {
                // 优先检查是否满足请求长度 (固定 8 字节)
                if (buf.readableBytes() >= 8) {
                    int byteCount = buf.getUnsignedByte(index + 2);
                    int respLen = 5 + byteCount;
                    // 如果长度刚好匹配响应且不是 8, 则按响应处理
                    if (buf.readableBytes() == respLen && respLen != 8) {
                        return buf.readRetainedSlice(respLen);
                    }
                    // 否则按 8 字节截断 (请求)
                    return buf.readRetainedSlice(8);
                }

                // 如果长度小于 8, 检查是否满足响应长度.
                // 注意: 必须防止残缺请求 (如 01 03 00 00 00) 被误判为响应.
                // 由于请求固定为 8 字节, 任何小于 8 字节的报文如果满足响应长度且不是请求的前缀(其实请求前缀也可能满足),
                // 我们增加一个限制: 只有在确定不可能是请求的一部分时才返回.
                // 或者简单点: 在此规则中, 01-04 响应长度必须满足 respLen, 且由于 < 8, 所以 respLen 只能是 5, 6, 7.
                // 而 01 03 00 00 00 满足 respLen=5 (byteCount=0), 这确实很难区分.
                // 考虑到 RTU 的特性, 通常会有时间间隔. 这里我们只能根据字节数来猜.
                if (buf.readableBytes() >= 3) {
                    int byteCount = buf.getUnsignedByte(index + 2);
                    int respLen = 5 + byteCount;
                    if (buf.readableBytes() == respLen) {
                        // 额外校验: 如果 byteCount 为 0, 对于 01-04 响应来说不太常见(读取0个寄存器?),
                        // 而对于请求前缀 01 03 00 00 00 来说刚好满足.
                        // 所以如果 byteCount == 0 且长度 < 8, 我们暂不认为是完整帧.
                        if (byteCount == 0) {
                            return null;
                        }
                        return buf.readRetainedSlice(respLen);
                    }
                }

                return null;
            }

            // ======================= 05,06: 固定 8 字节 =======================
            case 5:
            case 6: {
                int frameLength = 8;
                if (buf.readableBytes() < frameLength) {
                    return null;
                }
                return buf.readRetainedSlice(frameLength);
            }

            // ======================= 0F,10: 写多个 =======================
            case 15:
            case 16: {
                // 响应固定 8 字节: 地址(1)+功能码(1)+起始(2)+数量(2)+CRC(2)
                if (buf.readableBytes() == 8) {
                    return buf.readRetainedSlice(8);
                }

                // 请求: 地址(1)+功能码(1)+起始(2)+数量(2)+字节数(1)+数据(N)+CRC(2)
                // 最小长度: 9
                if (buf.readableBytes() >= 9) {
                    int byteCount = buf.getUnsignedByte(index + 6);
                    int reqLen = 6 + 1 + byteCount + 2;
                    if (buf.readableBytes() == reqLen) {
                        return buf.readRetainedSlice(reqLen);
                    }
                }
                return null;
            }
            default:
                break;
        }

        return null;
    }
}

