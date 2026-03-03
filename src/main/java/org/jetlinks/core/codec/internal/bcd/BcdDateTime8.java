package org.jetlinks.core.codec.internal.bcd;

import io.netty.buffer.ByteBuf;
import org.jetlinks.core.codec.Codec;
import org.jspecify.annotations.NonNull;

import java.time.LocalDateTime;

import static org.jetlinks.core.codec.internal.bcd.BcdUtils.*;

/**
 * 基于 8 字节 BCD 编码的日期时间编解码器.
 * 常用于 Modbus 等工业协议, 保持 16 位寄存器对齐.
 * <p>
 * 编码规则:
 * <pre>{@code
 * 1: 年 (16bit Packed BCD) 0x2024 -> 2024
 * 2: 月/日 (16bit Packed BCD) 0x0315 -> 3月15日
 * 3: 时/分 (16bit Packed BCD) 0x1430 -> 14:30
 * 4: 秒 (16bit Packed BCD) 0x0025 -> 25秒
 * }</pre>
 *
 * @author zhouhao
 * @since 1.2
 */
public class BcdDateTime8 implements Codec<LocalDateTime> {

    @Override
    public Class<LocalDateTime> forType() {
        return LocalDateTime.class;
    }


    @Override
    public String getId() {
        return "bcd_date_time_8";
    }


    @Override
    public int byteLength() {
        return 8;
    }

    @Override
    public LocalDateTime decode(@NonNull ByteBuf payload) {
        int year = decodeBcd(payload.readShort());        // 年(16bit BCD)
        int monthDay = decodeBcd(payload.readShort());    // 月/日(16bit BCD)
        int hourMinute = decodeBcd(payload.readShort());  // 时/分(16bit BCD)
        int second = decodeBcd(payload.readShort());      // 秒(16bit BCD)

        return LocalDateTime.of(
            year,
            monthDay / 100,
            monthDay % 100,
            hourMinute / 100,
            hourMinute % 100,
            second);
    }


    @Override
    public ByteBuf encode(LocalDateTime body, ByteBuf buf) {
        buf.writeShort(encodeBcd(body.getYear()));        // 年(16bit BCD)
        buf.writeShort(encodeBcd(body.getMonthValue() * 100 + body.getDayOfMonth())); // 月/日(16bit BCD)
        buf.writeShort(encodeBcd(body.getHour() * 100 + body.getMinute()));           // 时/分(16bit BCD)
        buf.writeShort(encodeBcd(body.getSecond()));      // 秒(16bit BCD)

        return buf;
    }

}
