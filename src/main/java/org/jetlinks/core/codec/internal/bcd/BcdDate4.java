package org.jetlinks.core.codec.internal.bcd;

import io.netty.buffer.ByteBuf;
import org.jetlinks.core.codec.Codec;
import org.jspecify.annotations.NonNull;

import java.time.LocalDate;

import static org.jetlinks.core.codec.internal.bcd.BcdUtils.*;

/**
 * 基于 4 字节 BCD 编码的日期编解码器.
 * 常见的 4 字节格式为: YYYYMMDD (各占 1 字节, 年份占 2 字节).
 * 常用于电力、水务等行业协议.
 * <p>
 * 编码规则:
 * <pre>{@code
 * 1: 年 (16bit Packed BCD) 0x2024 -> 2024
 * 2: 月 (8bit Packed BCD) 0x03 -> 3月
 * 3: 日 (8bit Packed BCD) 0x15 -> 15日
 * }</pre>
 *
 * @author zhouhao
 * @since 1.2
 */
public class BcdDate4 implements Codec<LocalDate> {

    @Override
    public Class<LocalDate> forType() {
        return LocalDate.class;
    }


    @Override
    public String getId() {
        return "bcd_date_4";
    }


    @Override
    public int byteLength() {
        return 4;
    }

    @Override
    public LocalDate decode(@NonNull ByteBuf payload) {
        int year = decodeBcd(payload.readShort());       // 年(16bit BCD)
        int month = decodeBcd(payload.readByte());        // 月(8bit BCD)
        int day = decodeBcd(payload.readByte());          // 日(8bit BCD)

        return LocalDate.of(year, month, day);
    }


    @Override
    public ByteBuf encode(LocalDate body, ByteBuf buf) {
        buf.writeShort(encodeBcd(body.getYear()));       // 年(16bit BCD)
        buf.writeByte(encodeBcd8(body.getMonthValue())); // 月(8bit BCD)
        buf.writeByte(encodeBcd8(body.getDayOfMonth())); // 日(8bit BCD)

        return buf;
    }

}
