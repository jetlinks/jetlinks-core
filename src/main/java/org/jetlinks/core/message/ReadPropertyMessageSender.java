package org.jetlinks.core.message;

import org.jetlinks.core.message.property.ReadPropertyMessageReply;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletionStage;

/**
 * @author zhouhao
 * @since 1.0.0
 */
public interface ReadPropertyMessageSender {

    default ReadPropertyMessageSender read(String... property) {
        return read(Arrays.asList(property));
    }

    ReadPropertyMessageSender messageId(String messageId);

    ReadPropertyMessageSender read(List<String> properties);

    CompletionStage<ReadPropertyMessageReply> send();

}
