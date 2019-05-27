package org.jetlinks.core.message;

import org.jetlinks.core.message.property.WritePropertyMessageReply;

import java.util.Map;
import java.util.concurrent.CompletionStage;

/**
 * @author zhouhao
 * @since 1.0.0
 */
public interface WritePropertyMessageSender {

    WritePropertyMessageSender write(String property, Object value);


   default WritePropertyMessageSender write(Map<String,Object> properties){
       properties.forEach(this::write);
       return this;
   }

    CompletionStage<WritePropertyMessageReply> send();

}
