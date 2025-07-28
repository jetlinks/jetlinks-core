package org.jetlinks.core.codec;

import lombok.extern.slf4j.Slf4j;
import org.hswebframework.web.exception.BusinessException;
import org.jetlinks.core.cache.Caches;
import org.jetlinks.core.codec.internal.*;
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
        Codec<Byte> INT8 = new Int8();
        Codec<Short> INT16 = new Int16();
        Codec<Number> UnsignedInt16 = new UnsignedInt16();
        Codec<Number> UnsignedInt32 = new UnsignedInt32();
        Codec<Integer> INT32 = new Int32();
        Codec<Long> INT64 = new Int64();

        Codec<Float> Ieee754Float32 = new Ieee754Float32();
        Codec<Double> Ieee754Float64 = new Ieee754Float64();

        Codec<Float> Q1_15 = new FixedPointQ1_15();
        Codec<Float> Q1_31 = new FixedPointQ1_31();
        Codec<Float> Q8_8 = new FixedPointQ8_8();
        Codec<Float> Q15_1 = new FixedPointQ15_1();
        Codec<Float> Q31_1 = new FixedPointQ31_1();
        Codec<Float> FixedPointScaled10 = new FixedPointScaled10();
    }

    private static Map<String, Codec<?>> mapping = new ConcurrentHashMap<>();

    static {
        register(
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
            Internal.FixedPointScaled10
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
