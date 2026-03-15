package org.jetlinks.core.message.codec.parser;

import io.netty.buffer.ByteBuf;

import java.util.List;

/**
 * 通用 Modbus TCP 粘拆包解析器.
 * <p>
 * 适用于同一条 TCP 连接上同时承载:
 * <ul>
 *     <li>设备注册帧</li>
 *     <li>心跳帧</li>
 *     <li>Modbus TCP 业务透传帧</li>
 * </ul>
 *
 * 只要这些帧都遵循 Modbus TCP 的 MBAP 报文头格式:
 *
 * <pre>
 * +------------+------------+------------+------------+----------+------+
 * | Transaction| Protocol   | Length     | Unit Id    |  PDU ... |
 * | Identifier | Identifier | (2 bytes)  | (1 byte)   |          |
 * |   2 bytes  |  2 bytes   |            |            |          |
 * +------------+------------+------------+------------+----------+------+
 * </pre>
 *
 * 其中 Length 字段表示后续字节数 (Unit Id + PDU), 则完整帧总长度为:
 * <pre>
 *  frameLength = 6 (MBAP 头部字节数) + lengthField
 * </pre>
 *
 * 本解析器仅负责基于上述长度字段进行粘拆包, 并不区分注册帧、心跳帧或业务帧,
 * 上层可根据功能码、地址等字段自行识别帧类型。
 *
 * @author zhouhao
 * @since 1.3.2
 */
public class ModbusTcpMessageParser extends AbstractMessageParser {

    private static final int MBAP_HEADER_SIZE = 6;

    private static final int LENGTH_FIELD_OFFSET = 4;

    private static final int MIN_FRAME_BYTES = MBAP_HEADER_SIZE + 1;

    @Override
    protected void handle(ByteBuf buf, List<ByteBuf> container) {
        // 可读数据不足以解析 MBAP 头, 等待更多数据
        if (!buf.isReadable(MIN_FRAME_BYTES)) {
            return;
        }

        int readerIndex = buf.readerIndex();

        int lengthField = buf.getUnsignedShort(readerIndex + LENGTH_FIELD_OFFSET);

        int frameLength = MBAP_HEADER_SIZE + lengthField;

        if (!buf.isReadable(frameLength)) {
            // 半包，等待更多数据
            return;
        }

        // 从累积缓冲区中读出一个完整的 Modbus TCP 帧
        ByteBuf frame = buf.readRetainedSlice(frameLength);
        container.add(frame);
    }
}

