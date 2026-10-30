package org.jetlinks.core.codec.internal;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import org.jetlinks.core.codec.Codec;

import javax.annotation.Nonnull;

/**
 * 将原始字节与十六进制字符串互转，供点位上报和存储使用。
 * 解码消费全部可读字节，输出无前缀、无分隔符的小写字符串并保留前导零；
 * 编码接受大小写十六进制字符，每两个字符对应一个字节。
 * 字节布局由调用方在解码前、编码后调整，与其他点位编解码器一致。
 * 实例无状态，可共享；缓冲区由调用方释放。
 *
 * @see Codec
 * @since 1.3.3
 */
public class HexCodec implements Codec<String> {

    @Override
    public Class<String> forType() {
        return String.class;
    }

    @Override
    public String getId() {
        return "hex";
    }

    @Override
    public int byteLength() {
        return -1;
    }

    @Override
    public String decode(@Nonnull ByteBuf payload) {
        String hex = ByteBufUtil.hexDump(payload);
        payload.skipBytes(payload.readableBytes());
        return hex;
    }

    @Override
    public ByteBuf encode(String body, ByteBuf buf) {
        return buf.writeBytes(ByteBufUtil.decodeHexDump(body));
    }
}
