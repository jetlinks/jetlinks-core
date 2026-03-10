package org.jetlinks.core.message.codec;

import org.jetlinks.core.message.codec.parser.MessageParserBuilder;
import reactor.core.Disposable;

import java.util.List;

/**
 * 自定义报文解析规则. 用于协议包自定义TCP粘拆包规则等场景.
 *
 * @author zhouhao
 * @since 1.3.2
 */
public interface MessageParser extends Disposable {

    /**
     * 处理报文.返回处理后的结果.
     *
     * @param message 报文
     * @return 报文列表
     */
    List<? extends EncodedMessage> handle(EncodedMessage message);

    /**
     * 断开链接时被调用,释放资源.
     */
    @Override
    void dispose();

    static MessageParserBuilder builder() {
        return MessageParserBuilder.builder();
    }
}
