package org.jetlinks.core.codec.internal;

import io.netty.buffer.ByteBuf;
import org.hswebframework.web.exception.BusinessException;
import org.jetlinks.core.codec.Codec;

import javax.annotation.Nonnull;
import java.math.BigInteger;

/**
 * 无符号 64 位整数编解码器，每个值占 8 字节。
 * <p>
 * 使用 {@link BigInteger} 表示 0 到 2^64-1，避免最高位为 1 时被解释为负数。
 * 由 {@link org.jetlinks.core.codec.Codecs} 复用，实例无状态；字节布局由调用方调整，
 * 本类不释放传入的缓冲区。编码拒绝超范围值，避免静默截断。
 *
 * @since 1.3.3
 */
public class UnsignedInt64 implements Codec<BigInteger> {
    @Override
    public Class<BigInteger> forType() {
        return BigInteger.class;
    }

    @Override
    public String getId() {
        return "unsigned_int64";
    }

    @Override
    public int byteLength() {
        return 8;
    }

    @Override
    public BigInteger decode(@Nonnull ByteBuf payload) {
        long bits = payload.readLong();
        BigInteger value = BigInteger.valueOf(bits & Long.MAX_VALUE);
        return bits < 0 ? value.setBit(Long.SIZE - 1) : value;
    }

    @Override
    public ByteBuf encode(BigInteger body, ByteBuf buf) {
        if (body.signum() < 0 || body.bitLength() > Long.SIZE) {
            throw new BusinessException.NoStackTrace("error.codec.unsigned_int64.out_of_range", 400, body);
        }
        // longValue 保留低 64 位；范围检查保证这里不会丢失无符号整数的数据。
        return buf.writeLong(body.longValue());
    }
}
