package org.jetlinks.core.ipc;


import org.jetlinks.core.codec.Codec;
import org.jetlinks.core.codec.Codecs;
import org.jetlinks.core.codec.defaults.ErrorCodec;

/**
 * IPC定义信息
 *
 * @param <REQ> 请求类型
 * @param <RES> 响应类型
 */
public interface IpcDefinition<REQ, RES> {

    /**
     * 通信地址
     *
     * @return 地址
     */
    String getAddress();

    /**
     * @return 请求编解码器
     */
    Codec<REQ> requestCodec();

    /**
     * @return 响应编解码器
     */
    Codec<RES> responseCodec();

    /**
     * @return 错误相应编解码器
     */
    default Codec<Throwable> errorCodec() {
        return ErrorCodec.DEFAULT;
    }

    static <REQ, RES> IpcDefinition<REQ, RES> of(String address,
                                                 Codec<REQ> requestCodec,
                                                 Codec<RES> responseCodec) {
        return new DefaultIpcDefinition<>(address, requestCodec, responseCodec);
    }

    static IpcDefinition<Void, Void> of(String address) {
        return new DefaultIpcDefinition<>(address, Codecs.lookup(Void.class), Codecs.lookup(Void.class));
    }

    static <REQ, RES> IpcDefinition<REQ, RES> of(String address,
                                                 Class<REQ> requestType,
                                                 Class<RES> responseType) {
        return new DefaultIpcDefinition<>(address, Codecs.lookup(requestType), Codecs.lookup(responseType));
    }

}
