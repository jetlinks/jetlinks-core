package org.jetlinks.core.message.codec;

import org.jetlinks.core.server.ClientConnection;
import reactor.core.publisher.Mono;

/**
 * 消息解析起工厂,用于根据连接信息创建消息解析器,解析器用来识别和解析消息,处理粘拆包等.
 *
 * @author zhouhao
 * @since 1.3.2
 */
public interface MessageParserFactory {

    /**
     * 根据连接信息创建消息解析器
     *
     * @param connection 连接信息
     * @return 消息解析器
     * @see MessageParser
     * @see MessageParser#builder()
     */
    Mono<MessageParser> create(ClientConnection connection);
}
