package org.jetlinks.core.rpc;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import reactor.core.Disposable;


public interface SerializedContext extends Disposable {


    static SerializedContext empty() {
        return EmptySerializedContext.INSTANCE;
    }

    String getContext();

    /**
     * 将本次捕获结果追加到调用方的 writerIndex，不释放缓冲区。
     * 二进制实现直接覆写此方法；默认桥接既有文本实现，不创建中间 ByteBuf。
     * 写入失败时调用方丢弃本次帧；lease 仍由调用 owner 在终止时 dispose。
     */
    default void writeTo(ByteBuf target) {
        String text = getContext();
        if (text != null && !text.isEmpty()) ByteBufUtil.writeUtf8(target, text);
    }

    @Override
    default void dispose() {
    }
}
