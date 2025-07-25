package org.jetlinks.core.codec.layout;

import io.netty.buffer.ByteBuf;
import org.hswebframework.web.i18n.LocaleUtils;

/**
 * 字节布局接口，用于重新排列字节顺序
 *
 * <p>该接口定义了字节重排的标准操作，主要用于处理不同字节序（大小端）之间的转换。
 * 通过索引转换器数组来定义字节重排规则，支持2字节、4字节、8字节等多种布局。
 *
 * <p><strong>工作原理：</strong>
 * <ul>
 *   <li>每个布局都有一个索引转换器数组，定义了字节重排的规则</li>
 *   <li>索引转换器数组中的每个元素表示输出位置对应的输入位置</li>
 *   <li>例如：{1, 0} 表示将输入的第1个字节放在输出的第0个位置，输入的第0个字节放在输出的第1个位置</li>
 * </ul>
 *
 * <p><strong>预定义布局：</strong>
 * <ul>
 *   <li><strong>2字节布局：</strong> AB(正常), BA(交换)</li>
 *   <li><strong>4字节布局：</strong> AB_CD(正常), CD_AB(交换), BA_DC(部分交换), DA_BC(部分交换)</li>
 *   <li><strong>8字节布局：</strong> AB_CD_EF_GH(正常), GH_EF_CD_AB(交换), BA_DC_FE_HG(部分交换), HG_FE_DC_BA(完全反转)</li>
 * </ul>
 *
 * <p><strong>使用示例：</strong>
 * <pre>{@code
 * // 创建4字节布局，将CD放在前面，AB放在后面
 * ByteLayout cdAbLayout = ByteLayout.CD_AB;
 *
 * // 输入字节：[0x01, 0x02, 0x03, 0x04]
 * ByteBuf input = Unpooled.wrappedBuffer(new byte[]{0x01, 0x02, 0x03, 0x04});
 *
 * // 重排字节
 * ByteBuf result = cdAbLayout.reorder(input);
 * // 结果：[0x03, 0x04, 0x01, 0x02]
 * }</pre>
 *
 * @author zhouhao
 * @see ByteLayoutImpl
 * @since 1.2.4
 */
public interface ByteLayout {

    //@formatter:off
     ByteLayout
        // 2字节
        AB = ByteLayouts.AB,
        BA = ByteLayouts.BA,
        // 4 字节
        AB_CD = ByteLayouts.AB_CD,
        CD_AB = ByteLayouts.CD_AB,
        BA_DC = ByteLayouts.BA_DC,
        DC_BA = ByteLayouts.DC_BA,

         // 8字节
        AB_CD_EF_GH = ByteLayouts.AB_CD_EF_GH,
        GH_EF_CD_AB = ByteLayouts.GH_EF_CD_AB,
        BA_DC_FE_HG = ByteLayouts.BA_DC_FE_HG,
        HG_FE_DC_BA = ByteLayouts.HG_FE_DC_BA
    ;
    //@formatter:on


    static ByteLayout create(String id, int[] layout) {
        return new ByteLayoutImpl(id, layout);
    }

    /**
     * 获取布局的唯一标识符
     *
     * <p>返回布局的字符串标识符，用于区分不同的字节布局。
     * 例如："AB"、"BA"、"CD_AB"、"HG_FE_DC_BA"等。
     *
     * @return 布局的唯一标识符
     */
    String getId();

    /**
     * 布局名称
     *
     * @return 名称
     */
    default String getName() {
        return LocaleUtils.resolveMessage(
            "message.byte.layout." + getId()+".name",
            getId());
    }

    /**
     * 获取布局的字节长度
     *
     * <p>返回该布局处理的字节数量。不同的布局支持不同的字节长度：
     * <ul>
     *   <li>2字节布局：返回 2</li>
     *   <li>4字节布局：返回 4</li>
     *   <li>8字节布局：返回 8</li>
     * </ul>
     *
     * @return 布局的字节长度
     */
    int byteLength();

    /**
     * 重新排列字节顺序
     *
     * <p>根据布局的索引转换器规则，重新排列输入字节缓冲区的字节顺序。
     * 该方法会直接修改输入的ByteBuf，并返回同一个ByteBuf实例以支持链式调用。
     *
     * <p><strong>操作说明：</strong>
     * <ul>
     *   <li>读取输入字节缓冲区中的所有字节</li>
     *   <li>根据索引转换器数组重新排列字节</li>
     *   <li>将重排后的字节写回原缓冲区</li>
     *   <li>返回修改后的缓冲区</li>
     * </ul>
     *
     * <p><strong>异常情况：</strong>
     * <ul>
     *   <li>如果输入为null，抛出 {@link IllegalArgumentException}</li>
     *   <li>如果输入字节数不足，抛出 {@link IllegalArgumentException}</li>
     *   <li>如果索引转换器无效，抛出 {@link IndexOutOfBoundsException}</li>
     * </ul>
     *
     * @param byteBuf 输入字节缓冲区（不会被自动释放）
     * @return 重排后的字节缓冲区（支持链式调用）
     * @throws IllegalArgumentException  如果输入为null或字节数不足
     * @throws IndexOutOfBoundsException 如果索引转换器无效
     */
    ByteBuf reorder(ByteBuf byteBuf);


}
