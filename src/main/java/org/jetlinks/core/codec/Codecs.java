package org.jetlinks.core.codec;

import lombok.extern.slf4j.Slf4j;
import org.hswebframework.web.exception.BusinessException;
import org.jetlinks.core.cache.Caches;
import org.jetlinks.core.codec.internal.*;
import org.jetlinks.core.codec.internal.arrays.ArrayCodec;
import org.jetlinks.core.codec.internal.arrays.BitArray;
import org.jetlinks.core.codec.internal.bcd.*;
import org.reactivestreams.Publisher;
import org.springframework.core.ResolvableType;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 编解码支持
 *
 * @author zhouhao
 * @see Codecs.Internal
 * @see Codecs#getNow(String)
 * @since 1.2
 */
@SuppressWarnings("all")
@Slf4j
public final class Codecs {

    public interface Internal {
        Codec<Boolean> BOOL = new Bool();
        Codec<Boolean[]> BOOL_ARRAY = new ArrayCodec<>(BOOL);
        Codec<Byte> INT8 = new Int8();
        Codec<Byte[]> INT8_ARRAY = new ArrayCodec<>(INT8);
        Codec<Short> INT16 = new Int16();
        Codec<Short[]> INT16_ARRAY = new ArrayCodec<>(INT16);
        Codec<Number> UnsignedInt16 = new UnsignedInt16();
        Codec<Number[]> UnsignedInt16_ARRAY = new ArrayCodec<>(UnsignedInt16);
        Codec<Number> UnsignedInt32 = new UnsignedInt32();
        Codec<Number[]> UnsignedInt32_ARRAY = new ArrayCodec<>(UnsignedInt32);
        Codec<Integer> INT32 = new Int32();
        Codec<Integer[]> INT32_ARRAY = new ArrayCodec<>(INT32);
        Codec<Long> INT64 = new Int64();
        Codec<Long[]> INT64_ARRAY = new ArrayCodec<>(INT64);

        Codec<Float> Ieee754Float32 = new Ieee754Float32();
        Codec<Float[]> Ieee754Float32_ARRAY = new ArrayCodec<>(Ieee754Float32);
        Codec<Double> Ieee754Float64 = new Ieee754Float64();
        Codec<Double[]> Ieee754Float64_ARRAY = new ArrayCodec<>(Ieee754Float64);

        Codec<Float> Q1_15 = new FixedPointQ1_15();
        Codec<Float[]> Q1_15_ARRAY = new ArrayCodec<>(Q1_15);
        Codec<Float> Q1_31 = new FixedPointQ1_31();
        Codec<Float[]> Q1_31_ARRAY = new ArrayCodec<>(Q1_31);
        Codec<Float> Q8_8 = new FixedPointQ8_8();
        Codec<Float[]> Q8_8_ARRAY = new ArrayCodec<>(Q8_8);
        Codec<Float> Q15_1 = new FixedPointQ15_1();
        Codec<Float[]> Q15_1_ARRAY = new ArrayCodec<>(Q15_1);
        Codec<Float> Q31_1 = new FixedPointQ31_1();
        Codec<Float[]> Q31_1_ARRAY = new ArrayCodec<>(Q31_1);
        Codec<Float> FixedPointScaled10 = new FixedPointScaled10();
        Codec<Float[]> FixedPointScaled10_ARRAY = new ArrayCodec<>(FixedPointScaled10);

        /**
         * 8 位 Packed BCD 编解码器.
         * @see Bcd8
         */
        Codec<Integer> BCD8 = new Bcd8();
        Codec<Integer[]> BCD8_ARRAY = new ArrayCodec<>(BCD8);

        /**
         * 16 位 Packed BCD 编解码器.
         * @see Bcd16
         */
        Codec<Integer> BCD16 = new Bcd16();
        Codec<Integer[]> BCD16_ARRAY = new ArrayCodec<>(BCD16);

        /**
         * 32 位 Packed BCD 编解码器.
         * @see Bcd32
         */
        Codec<Integer> BCD32 = new Bcd32();
        Codec<Integer[]> BCD32_ARRAY = new ArrayCodec<>(BCD32);

        /**
         * 16 位 Unpacked BCD 编解码器.
         * @see UnpackedBcd16
         */
        Codec<Integer> UnpackedBCD16 = new UnpackedBcd16();
        Codec<Integer[]> UnpackedBCD16_ARRAY = new ArrayCodec<>(UnpackedBCD16);

        /**
         * 32 位 Unpacked BCD 编解码器.
         * @see UnpackedBcd32
         */
        Codec<Integer> UnpackedBCD32 = new UnpackedBcd32();
        Codec<Integer[]> UnpackedBCD32_ARRAY = new ArrayCodec<>(UnpackedBCD32);

        /**
         * 8 字节 BCD 日期时间编解码器.
         * @see BcdDateTime8
         */
        Codec<java.time.LocalDateTime> BCD_DATE_TIME_8 = new BcdDateTime8();

        /**
         * 12 字节 BCD 日期时间编解码器.
         * @see BcdDateTime12
         */
        Codec<java.time.LocalDateTime> BCD_DATE_TIME_12 = new BcdDateTime12();

        Codec<Boolean[]> BIT_ARRAY = new BitArray();
    }

    private static Map<String, Codec<?>> mapping = new ConcurrentHashMap<>();

    static {
        register(
            // 基础类型
            Internal.BOOL,
            Internal.INT8,
            Internal.INT16,
            Internal.UnsignedInt16,
            Internal.UnsignedInt32,
            Internal.INT32,
            Internal.INT64,
            Internal.Ieee754Float32,
            Internal.Ieee754Float64,
            Internal.Q1_15,
            Internal.Q1_31,
            Internal.Q8_8,
            Internal.Q15_1,
            Internal.Q31_1,
            Internal.FixedPointScaled10,
            // BCD
            Internal.BCD8,
            Internal.BCD16,
            Internal.BCD32,
            Internal.UnpackedBCD16,
            Internal.UnpackedBCD32,
            Internal.BCD_DATE_TIME_8,
            Internal.BCD_DATE_TIME_12,
            // 数组类型
            Internal.BOOL_ARRAY,
            Internal.INT8_ARRAY,
            Internal.INT16_ARRAY,
            Internal.UnsignedInt16_ARRAY,
            Internal.UnsignedInt32_ARRAY,
            Internal.INT32_ARRAY,
            Internal.INT64_ARRAY,
            Internal.Ieee754Float32_ARRAY,
            Internal.Ieee754Float64_ARRAY,
            Internal.Q1_15_ARRAY,
            Internal.Q1_31_ARRAY,
            Internal.Q8_8_ARRAY,
            Internal.Q15_1_ARRAY,
            Internal.Q31_1_ARRAY,
            Internal.FixedPointScaled10_ARRAY,
            // BCD 数组
            Internal.BCD8_ARRAY,
            Internal.BCD16_ARRAY,
            Internal.BCD32_ARRAY,
            Internal.UnpackedBCD16_ARRAY,
            Internal.UnpackedBCD32_ARRAY,
            // 位数组
            Internal.BIT_ARRAY
        );
    }

    public static final void register(Codec<?>... codec) {
        for (Codec<?> codec1 : codec) {
            mapping.put(codec1.getId(), codec1);
        }
    }

    public static final Codec<?> getNow(String id) {
        return get(id)
            .orElseThrow(() -> new BusinessException.NoStackTrace("error.unsupported_codec", id));
    }

    public static final Optional<Codec<?>> get(String id) {
        return Optional.ofNullable(mapping.get(id));
    }


    public static final List<Codec<?>> getAll() {
        return new ArrayList<>(mapping.values());
    }

}
