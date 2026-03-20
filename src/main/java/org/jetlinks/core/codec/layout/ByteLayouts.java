package org.jetlinks.core.codec.layout;

import org.hswebframework.web.exception.BusinessException;
import org.jetlinks.core.codec.Codec;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static org.jetlinks.core.codec.layout.ByteLayout.*;

public class ByteLayouts {
    static final int A = 0, B = 1, C = 2, D = 3, E = 4, F = 5, G = 6, H = 7;

    //@formatter:off
    static final ByteLayout
        // 2字节
        AB = new DirectByteLayout("AB",2),
        BA = create("BA", new int[]{B, A}),
        // 4 字节
        AB_CD =new DirectByteLayout("AB_CD",4),
        CD_AB = create("CD_AB", new int[]{C, D, A, B}),
        BA_DC = create("BA_DC", new int[]{B, A, D, C}),
        DC_BA = create("DC_BA", new int[]{D, C, B, A}),

        // 8字节
        AB_CD_EF_GH = new DirectByteLayout("AB_CD_EF_GH",8),
        GH_EF_CD_AB = create("GH_EF_CD_AB", new int[]{G, H, E, F, C, D, A, B}),
        BA_DC_FE_HG = create("BA_DC_FE_HG", new int[]{B, A, D, C, F, E, H, G}),
        HG_FE_DC_BA = create("HG_FE_DC_BA", new int[]{H, G, F, E, D, C, B, A}),

        FE_HG_BA_DC = create("FE_HG_BA_DC", new int[]{F, E, H, G, B, A, D, C}),
        DC_BA_HG_FE = create("DC_BA_HG_FE", new int[]{D, C, B, A, H, G, F, E}),

        BIG_ENDIAN = new DirectByteLayout("BIG_ENDIAN", -1),
        LITTLE_ENDIAN = new ReverseByteLayout("LITTLE_ENDIAN", -1),
        WORD_SWAP_2 = new WordSwapByteLayout("WORD_SWAP_2", -1, 2),
        WORD_REVERSE_2 = new WordReverseByteLayout("WORD_REVERSE_2", -1, 2)
            ;
    //@formatter:on

    private static final Map<String, ByteLayout> layouts = new ConcurrentHashMap<>();

    static {
        register(
            AB,
            BA,
            AB_CD,
            CD_AB,
            BA_DC,
            DC_BA,
            AB_CD_EF_GH,
            GH_EF_CD_AB,
            BA_DC_FE_HG,
            HG_FE_DC_BA,
            FE_HG_BA_DC,
            DC_BA_HG_FE,
            BIG_ENDIAN,
            LITTLE_ENDIAN,
            WORD_SWAP_2,
            WORD_REVERSE_2
        );
    }

    public static void register(ByteLayout... layouts) {
        for (ByteLayout layout : layouts) {
            ByteLayouts.layouts.put(layout.getId(), layout);
        }

    }

    public static   ByteLayout getNow(String id) {
        return get(id)
            .orElseThrow(() -> new BusinessException.NoStackTrace("error.unsupported_byte_layout", id));
    }

    public static Optional<ByteLayout> get(String id) {
        return Optional.ofNullable(layouts.get(id));
    }

    public static List<ByteLayout> getAll() {
        return new ArrayList<>(layouts.values());
    }

}
