package org.jetlinks.core.codec.internal.bcd;

import io.netty.buffer.ByteBuf;
import org.jetlinks.core.codec.Codec;
import org.jspecify.annotations.NonNull;

import java.time.LocalDateTime;

import static org.jetlinks.core.codec.internal.bcd.BcdUtils.*;

/**
 * 基于 7 字节 BCD 编码的日期时间编解码器.
 * 常见的 7 字节格式为: YYMMDDHHmmssww (或 YYMMDDwwHHmmss, 具体取决于协议, 此处采用常见电力/车载协议顺序: YYMMDDwwHHmmss).
 * <p>
 * 编码规则:
 * <pre>{@code
 * 1: 年 (8bit Packed BCD) 0x24 -> 2024
 * 2: 月 (8bit Packed BCD) 0x03 -> 3月
 * 3: 日 (8bit Packed BCD) 0x15 -> 15日
 * 4: 星期 (8bit Packed BCD) 0x01 -> 周一 (通常 01-07, 07 表示周日)
 * 5: 时 (8bit Packed BCD) 0x14 -> 14时
 * 6: 分 (8bit Packed BCD) 0x30 -> 30分
 * 7: 秒 (8bit Packed BCD) 0x25 -> 25秒
 * }</pre>
 *
 * @author zhouhao
 * @since 1.2
 */
public class BcdDateTime7 implements Codec<LocalDateTime> {

    @Override
    public Class<LocalDateTime> forType() {
        return LocalDateTime.class;
    }


    @Override
    public String getId() {
        return "bcd_date_time_7";
    }


    @Override
    public int byteLength() {
        return 7;
    }

    @Override
    public LocalDateTime decode(@NonNull ByteBuf payload) {
        int year = decodeBcd(payload.readByte());        // 年(8bit BCD)
        int month = decodeBcd(payload.readByte());       // 月(8bit BCD)
        int day = decodeBcd(payload.readByte());         // 日(8bit BCD)
        int week = decodeBcd(payload.readByte());        // 星期(8bit BCD), 忽略
        int hour = decodeBcd(payload.readByte());        // 时(8bit BCD)
        int minute = decodeBcd(payload.readByte());      // 分(8bit BCD)
        int second = decodeBcd(payload.readByte());      // 秒(8bit BCD)

        // 假设是 20xx 年
        return LocalDateTime.of(
            2000 + year,
            month,
            day,
            hour,
            minute,
            second);
    }


    @Override
    public ByteBuf encode(LocalDateTime body, ByteBuf buf) {
        buf.writeByte(encodeBcd8(body.getYear() % 100)); // 年(8bit BCD)
        buf.writeByte(encodeBcd8(body.getMonthValue())); // 月(8bit BCD)
        buf.writeByte(encodeBcd8(body.getDayOfMonth())); // 日(8bit BCD)

        // 星期: LocalDateTime.getDayOfWeek().getValue() (1:周一, 7:周日)
        buf.writeByte(encodeBcd8(body.getDayOfWeek().getValue()));

        buf.writeByte(encodeBcd8(body.getHour()));       // 时(8bit BCD)
        buf.writeByte(encodeBcd8(body.getMinute()));     // 分(8bit BCD)
        buf.writeByte(encodeBcd8(body.getSecond()));     // 秒(8bit BCD)

        return buf;
    }

}
