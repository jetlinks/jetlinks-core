package org.jetlinks.core.message.function;

import com.alibaba.fastjson.JSONObject;
import org.jetlinks.core.message.MessageType;
import org.jetlinks.core.message.RepayableThingMessage;
import org.jetlinks.core.things.ThingType;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 物功能调用消息
 *
 * @author zhouhao
 * @since 1.1.9
 */
public interface ThingFunctionInvokeMessage<R extends ThingFunctionInvokeMessageReply> extends RepayableThingMessage<R> {

    String getFunctionId();

    List<FunctionParameter> getInputs();

    ThingFunctionInvokeMessage<R> addInput(FunctionParameter parameter);

    @Override
    R newReply();

    default MessageType getMessageType() {
        return MessageType.INVOKE_FUNCTION;
    }

    default Optional<Object> getInput(String name) {
        return getInputs()
                .stream()
                .filter(param -> param.getName().equals(name))
                .map(FunctionParameter::getValue)
                .findFirst();
    }

    default Optional<Object> getInput(int index) {
        return getInputs().size() > index
                ? Optional.ofNullable(getInputs().get(index))
                : Optional.empty();
    }

    default Map<String, Object> inputsToMap() {
        return getInputs()
                .stream()
                .collect(Collectors.toMap(FunctionParameter::getName, FunctionParameter::getValue, (a, b) -> a));
    }

    default <T> T inputsToBean(Class<T> beanType) {
        return new JSONObject(inputsToMap())
                .toJavaObject(beanType);
    }

    default List<Object> inputsToList() {
        return getInputs().stream()
                          .map(FunctionParameter::getValue)
                          .collect(Collectors.toList());
    }

    default Object[] inputsToArray() {
        return getInputs().stream()
                          .map(FunctionParameter::getValue)
                          .toArray();
    }

    default ThingFunctionInvokeMessage<R> addInput(String name, Object value) {
        return this.addInput(new FunctionParameter(name, value));
    }

    static FunctionInvokeMessage forDevice(String deviceId) {
        FunctionInvokeMessage message = new FunctionInvokeMessage();
        message.setDeviceId(deviceId);
        return message;
    }

    static DefaultFunctionInvokeMessage forThing(ThingType thingType, String deviceId) {
        DefaultFunctionInvokeMessage message = new DefaultFunctionInvokeMessage();
        message.setThingId(deviceId);
        message.setThingType(thingType.getId());
        return message;
    }

}
