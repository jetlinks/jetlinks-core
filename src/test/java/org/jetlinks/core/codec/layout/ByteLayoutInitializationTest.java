package org.jetlinks.core.codec.layout;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

/**
 * 验证预定义字节布局不依赖接口与注册表的初始化顺序。
 */
public class ByteLayoutInitializationTest {

    @Test
    public void shouldInitializePublicLayoutsAfterRegistry() {
        // 必须先触发注册表初始化，复现生产调用中与接口常量相反的加载顺序。
        assertEquals(16, ByteLayouts.getAll().size());

        assertSame(ByteLayouts.getNow("AB"), ByteLayout.AB);
        assertSame(ByteLayouts.getNow("BA"), ByteLayout.BA);
        assertSame(ByteLayouts.getNow("AB_CD"), ByteLayout.AB_CD);
        assertSame(ByteLayouts.getNow("CD_AB"), ByteLayout.CD_AB);
        assertSame(ByteLayouts.getNow("BA_DC"), ByteLayout.BA_DC);
        assertSame(ByteLayouts.getNow("DC_BA"), ByteLayout.DC_BA);
        assertSame(ByteLayouts.getNow("AB_CD_EF_GH"), ByteLayout.AB_CD_EF_GH);
        assertSame(ByteLayouts.getNow("GH_EF_CD_AB"), ByteLayout.GH_EF_CD_AB);
        assertSame(ByteLayouts.getNow("BA_DC_FE_HG"), ByteLayout.BA_DC_FE_HG);
        assertSame(ByteLayouts.getNow("HG_FE_DC_BA"), ByteLayout.HG_FE_DC_BA);
        assertSame(ByteLayouts.getNow("FE_HG_BA_DC"), ByteLayout.FE_HG_BA_DC);
        assertSame(ByteLayouts.getNow("DC_BA_HG_FE"), ByteLayout.DC_BA_HG_FE);
        assertSame(ByteLayouts.getNow("BIG_ENDIAN"), ByteLayout.BIG_ENDIAN);
        assertSame(ByteLayouts.getNow("LITTLE_ENDIAN"), ByteLayout.LITTLE_ENDIAN);
        assertSame(ByteLayouts.getNow("WORD_SWAP_2"), ByteLayout.WORD_SWAP_2);
        assertSame(ByteLayouts.getNow("WORD_REVERSE_2"), ByteLayout.WORD_REVERSE_2);
    }
}
