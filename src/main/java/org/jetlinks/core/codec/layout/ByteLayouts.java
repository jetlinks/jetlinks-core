package org.jetlinks.core.codec.layout;

import org.hswebframework.web.exception.BusinessException;
import org.jetlinks.core.codec.Codec;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 预定义字节布局注册表。
 *
 * 内置布局实例由 {@link ByteLayout} 公共常量统一创建，本类只维护相同实例的 ID 索引，
 * 并通过并发 Map 支持运行时注册和查询，不参与具体字节重排。
 *
 * @see ByteLayout
 */
public class ByteLayouts {
    //@formatter:off
    static final ByteLayout
        // 2字节
        AB = ByteLayout.AB,
        BA = ByteLayout.BA,
        // 4 字节
        AB_CD = ByteLayout.AB_CD,
        CD_AB = ByteLayout.CD_AB,
        BA_DC = ByteLayout.BA_DC,
        DC_BA = ByteLayout.DC_BA,

        // 8字节
        AB_CD_EF_GH = ByteLayout.AB_CD_EF_GH,
        GH_EF_CD_AB = ByteLayout.GH_EF_CD_AB,
        BA_DC_FE_HG = ByteLayout.BA_DC_FE_HG,
        HG_FE_DC_BA = ByteLayout.HG_FE_DC_BA,

        FE_HG_BA_DC = ByteLayout.FE_HG_BA_DC,
        DC_BA_HG_FE = ByteLayout.DC_BA_HG_FE,

        BIG_ENDIAN = ByteLayout.BIG_ENDIAN,
        LITTLE_ENDIAN = ByteLayout.LITTLE_ENDIAN,
        WORD_SWAP_2 = ByteLayout.WORD_SWAP_2,
        WORD_REVERSE_2 = ByteLayout.WORD_REVERSE_2
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
