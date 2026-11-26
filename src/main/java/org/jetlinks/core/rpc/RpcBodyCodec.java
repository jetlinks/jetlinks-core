package org.jetlinks.core.rpc;
import io.netty.buffer.ByteBuf;
import java.lang.reflect.Type;
/** 对借用缓冲区读写业务对象，不释放缓冲区；ByteBuf 参数由运行时直接透传。 */
public interface RpcBodyCodec {
    String id();
    void encode(Object value, ByteBuf target);
    Object decode(ByteBuf source, Type type);
}
