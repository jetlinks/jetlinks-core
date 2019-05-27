package org.jetlinks.core.message;

import org.jetlinks.core.message.function.FunctionInvokeMessageReply;
import org.jetlinks.core.message.function.FunctionParameter;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;

/**
 * @author zhouhao
 * @since 1.0.0
 */
public interface FunctionInvokeMessageSender {

    FunctionInvokeMessageSender addParameter(String name, Object value);

    FunctionInvokeMessageSender setParameter(List<FunctionParameter> parameter);

    default FunctionInvokeMessageSender setParameter(Map<String, Object> parameter) {
        parameter.forEach(this::addParameter);
        return this;
    }

    FunctionInvokeMessageSender messageId(String messageId);

    FunctionInvokeMessageSender async();

    CompletionStage<FunctionInvokeMessageReply> send();

}
