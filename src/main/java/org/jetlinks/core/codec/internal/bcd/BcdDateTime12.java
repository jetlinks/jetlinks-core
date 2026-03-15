package org.jetlinks.core.codec.internal.bcd;

import io.netty.buffer.ByteBuf;
import org.jetlinks.core.codec.Codec;
import org.jspecify.annotations.NonNull;

import java.time.LocalDateTime;

import static org.jetlinks.core.codec.internal.bcd.BcdUtils.decodeBcd;
import static org.jetlinks.core.codec.internal.bcd.BcdUtils.encodeBcd;

/**
 * 基于 12 字节 BCD 编码的日期时间编解码器.
 * <p>
 * 编码规则:
 * 年、月、日、时、分、秒各占用 2 个字节 (16bit Packed BCD), 共 12 字节.
 *
 * @author zhouhao
 * @since 1.2
 */
public class BcdDateTime12 implements Codec<LocalDateTime> {

    @Override
    public Class<LocalDateTime> forType() {
        return LocalDateTime.class;
    }


    @Override
    public String getId() {
        return "bcd_date_time_12";
    }


    @Override
    public int byteLength() {
        return 12;
    }

    @Override
    public LocalDateTime decode(@NonNull ByteBuf payload) {
        return LocalDateTime.of(
            decodeBcd(payload.readShort()), // year
            decodeBcd(payload.readShort()), // month
            decodeBcd(payload.readShort()), // day
            decodeBcd(payload.readShort()), // hour
            decodeBcd(payload.readShort()), // minute
            decodeBcd(payload.readShort())  // second
        );
    }

    @Override
    public ByteBuf encode(LocalDateTime body, ByteBuf buf) {
        buf.writeShort(encodeBcd(body.getYear()));
        buf.writeShort(encodeBcd(body.getMonthValue()));
        buf.writeShort(encodeBcd(body.getDayOfMonth()));
        buf.writeShort(encodeBcd(body.getHour()));
        buf.writeShort(encodeBcd(body.getMinute()));
        buf.writeShort(encodeBcd(body.getSecond()));
        return buf;
    }

}
