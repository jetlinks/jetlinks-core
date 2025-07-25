package org.jetlinks.core.codec;

import io.netty.buffer.ByteBuf;
import org.hswebframework.web.i18n.LocaleUtils;
import org.jetlinks.core.buffer.Buffer;

import javax.annotation.Nonnull;

public interface Codec<T> {

    Class<T> forType();

    String getId();


    /**
     * 布局名称
     *
     * @return 名称
     */
    default String getName() {
        return LocaleUtils.resolveMessage(
            "message.codec." + getId() + ".name",
            getId());
    }

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
