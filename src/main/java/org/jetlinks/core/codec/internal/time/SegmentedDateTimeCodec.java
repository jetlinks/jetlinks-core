package org.jetlinks.core.codec.internal.time;

import io.netty.buffer.ByteBuf;
import org.jetlinks.core.codec.Codec;
import org.jspecify.annotations.NonNull;

import java.time.LocalDateTime;

/**
 * 分段日期时间编解码器.
 * <p>
 * 将年、月、日、时、分、秒分别使用 2 字节 (16bit 无符号短整数) 存储.
 * 总长度为 12 字节.
 *
 * @author zhouhao
 * @since 1.2
 */
public class SegmentedDateTimeCodec implements Codec<LocalDateTime> {

    @Override
    public Class<LocalDateTime> forType() {
        return LocalDateTime.class;
    }


    @Override
    public String getId() {
        return "seg_date_time";
    }


    @Override
    public int byteLength() {
        return 12;
    }


    @Override
    public LocalDateTime decode(@NonNull ByteBuf payload) {
        return LocalDateTime.of(
            payload.readUnsignedShort(), // year
            payload.readUnsignedShort(), // month
            payload.readUnsignedShort(), // day
            payload.readUnsignedShort(), // hour
            payload.readUnsignedShort(), // minute
            payload.readUnsignedShort()  // second
        );
    }


    @Override
    public ByteBuf encode(LocalDateTime body, ByteBuf buf) {
        buf.writeShort(body.getYear());
        buf.writeShort(body.getMonthValue());
        buf.writeShort(body.getDayOfMonth());
        buf.writeShort(body.getHour());
        buf.writeShort(body.getMinute());
        buf.writeShort(body.getSecond());
        return buf;
    }
}
