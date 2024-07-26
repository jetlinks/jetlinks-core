package org.jetlinks.core.message.command;

import org.hswebframework.web.bean.FastBeanCopier;
import org.jetlinks.core.command.AbstractCommand;
import org.jetlinks.core.command.CommandUtils;
import org.jetlinks.core.message.Message;
import org.jetlinks.core.message.MessageType;
import org.jetlinks.core.metadata.FunctionMetadata;
import org.jetlinks.core.metadata.SimpleFunctionMetadata;
import org.jetlinks.core.metadata.SimplePropertyMetadata;
import org.jetlinks.core.metadata.types.ObjectType;
import reactor.core.publisher.Flux;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 发送消息命令
 *
 * @author zhouhao
 * @see org.jetlinks.core.command.CommandSupport
 * @since 1.2.2
 */
public class SendMessageCommand extends AbstractCommand<Flux<Message>, SendMessageCommand> {

    public Message getMessage() {
        return convertMessage(readable().get("message"));
    }

    public SendMessageCommand setMessage(Message message) {
        return with("message", message);
    }

    @Override
    public Object createResponseData(Object value) {
        return convertMessage(value);
    }

    public static Message convertMessage(Object value) {
        if (value instanceof Message) {
            return (Message) value;
        }
        if (!(value instanceof Map)) {
            value = FastBeanCopier.copy(value, new HashMap<>());
        }
        Object fValue = value;
        @SuppressWarnings("all")
        Map<String, Object> mapValue = ((Map<String, Object>) value);

        return MessageType
            .convertMessage(mapValue)
            .orElseThrow(() -> new UnsupportedOperationException("unsupported data format:" + fValue));
    }


    public static FunctionMetadata metadata() {

        SimpleFunctionMetadata metadata = new SimpleFunctionMetadata();
        metadata.setId(CommandUtils.getCommandIdByType(SendMessageCommand.class));
        metadata.setName("发送消息");
        metadata.setInputs(Collections.singletonList(
            SimplePropertyMetadata.of("message", "消息内容", new ObjectType())));
        metadata.setOutput(new ObjectType());
        return metadata;
    }


}