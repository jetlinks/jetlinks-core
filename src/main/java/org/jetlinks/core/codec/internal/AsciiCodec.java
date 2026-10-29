package org.jetlinks.core.codec.internal;

import io.netty.buffer.ByteBuf;
import org.hswebframework.web.exception.BusinessException;
import org.jetlinks.core.codec.Codec;

import javax.annotation.Nonnull;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;

/**
 * 公共 US-ASCII 字符串编解码器，仅接受 0 到 127 范围内的字符和字节。
 * 解码以首个零字节为结束符，仍消费全部可读字节，保留结束符之前的空格；
 * 编码拒绝包含 NUL 的文本，长度适配、协议头和字节布局由调用方处理。
 * 由 {@link org.jetlinks.core.codec.Codecs} 共享，无状态，可并发调用。
 * 不释放缓冲区；字符校验失败时不改变读索引或写入部分数据。
 *
 * @see Codec
 * @since 1.3.3
 */
public class AsciiCodec implements Codec<String> {

    @Override
    public Class<String> forType() {
        return String.class;
    }

    @Override
    public String getId() {
        return "ascii";
    }

    @Override
    public int byteLength() {
        return -1;
    }

    @Override
    public String decode(@Nonnull ByteBuf payload) {
        int readerIndex = payload.readerIndex();
        int readableBytes = payload.readableBytes();
        // NUL 是字符串结束符，后续字节属于填充，不应进入解析值或存储链路。
        int textLength = payload.bytesBefore((byte) 0);
        if (textLength < 0) {
            textLength = readableBytes;
        }
        try {
            String value = StandardCharsets.US_ASCII
                .newDecoder()
                .decode(payload.nioBuffer(readerIndex, textLength))
                .toString();
            payload.skipBytes(readableBytes);
            return value;
        } catch (CharacterCodingException e) {
            throw new BusinessException.NoStackTrace("error.codec.ascii.invalid_character", 400);
        }
    }

    @Override
    public ByteBuf encode(String body, ByteBuf buf) {
        if (body.indexOf(0) >= 0) {
            throw new BusinessException.NoStackTrace("error.codec.ascii.null_character", 400);
        }
        try {
            // 完成严格字符校验后再写入目标缓冲区，避免非法文本留下部分写入的数据。
            return buf.writeBytes(StandardCharsets.US_ASCII.newEncoder().encode(CharBuffer.wrap(body)));
        } catch (CharacterCodingException e) {
            throw new BusinessException.NoStackTrace("error.codec.ascii.invalid_character", 400);
        }
    }
}
