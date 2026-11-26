package org.jetlinks.core.rpc;

import io.netty.buffer.ByteBuf;
import java.nio.charset.StandardCharsets;
import reactor.util.context.Context;
import reactor.util.context.ContextView;


public interface ContextCodec {

    ContextCodec DEFAULT = new ContextCodec() {
        @Override public String formatId() { return "none"; }
        @Override public Context deserializeFrom(Context source, ByteBuf buffer) {
            if (buffer.isReadable()) throw new IllegalArgumentException("RPC context codec is not configured");
            return source;
        }
    };

    /** 对端必须支持相同线格式；默认桥接既有 UTF-8 字符串，二进制实现覆写此标识和读入口。 */
    default String formatId() { return "jetlinks/context/text/1"; }

    default SerializedContext serialize(ContextView context) {
        return SerializedContext.empty();
    }

    default Context deserialize(Context source, SerializedContext ctx) {
        return source;
    }

    /** 同步读取借用的可读区，可推进 readerIndex，不 retain/release 或保留该缓冲区。 */
    default Context deserializeFrom(Context source, ByteBuf buffer) {
        if (!buffer.isReadable()) return source;
        String text = buffer.toString(StandardCharsets.UTF_8);
        return deserialize(source, () -> text);
    }


}
