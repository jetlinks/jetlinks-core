package org.jetlinks.core.codec.internal;

import io.netty.buffer.ByteBuf;
import io.netty.util.ByteProcessor;
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

    // byte 为有符号值，正文只允许 1..127，遇到 NUL 或非法字节时停止。
    private static final ByteProcessor FIND_ASCII_TEXT_END = value -> value > 0;

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
        int endIndex = payload.forEachByte(readerIndex, readableBytes, FIND_ASCII_TEXT_END);
        int textLength = readableBytes;
        if (endIndex >= 0) {
            // NUL 后的字节按填充处理；正文校验失败时不推进读索引。
            if (payload.getByte(endIndex) != 0) {
                throw new BusinessException.NoStackTrace("error.codec.ascii.invalid_character", 400);
            }
            textLength = endIndex - readerIndex;
        }
        String value = payload.toString(readerIndex, textLength, StandardCharsets.US_ASCII);
        payload.skipBytes(readableBytes);
        return value;
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
