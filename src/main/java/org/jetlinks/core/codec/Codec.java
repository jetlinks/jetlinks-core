package org.jetlinks.core.codec;

import io.netty.buffer.ByteBuf;
import org.hswebframework.web.i18n.LocaleUtils;

import javax.annotation.Nonnull;

/**
 * 编解码接口,提供对对象和{@link ByteBuf}之间的编解码支持.
 *
 * @param <T> 类型
 * @author zhouhao
 * @since 1.2
 */
public interface Codec<T> {

    /**
     * 编解码针对的类型
     *
     * @return 类型
     */
    Class<T> forType();

    /**
     * 编解码标识
     *
     * @return 标识
     */
    String getId();

    /**
     * 名称
     *
     * @return 名称
     */
    default String getName() {
        return LocaleUtils.resolveMessage(
            "message.codec." + getId() + ".name",
            getId());
    }

    /**
     * 字节长度,标识编解码将会操作几个字节的数据,如果是动态数据将返回-1
     *
     * @return 字节长度
     */
    int byteLength();

    /**
     * 解码数据,注意: 方法不会释放ByteBuf.
     *
     * @param payload ByteBuf
     * @return 解码结果
     */
    T decode(@Nonnull ByteBuf payload);

    /**
     * 编码数据
     *
     * @param body body
     * @param buf  ByteBuf
     * @return 解码结果
     */
    ByteBuf encode(T body, ByteBuf buf);

}
