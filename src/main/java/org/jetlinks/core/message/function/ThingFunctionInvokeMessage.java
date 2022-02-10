package org.jetlinks.core.message.function;

import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Maps;
import org.jetlinks.core.message.MessageType;
import org.jetlinks.core.message.RepayableThingMessage;
import org.jetlinks.core.metadata.FunctionMetadata;
import org.jetlinks.core.things.ThingType;
import org.jetlinks.core.utils.SerializeUtils;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
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

    /**
     * @return functionId
     * @see FunctionMetadata#getId()
     */
    String getFunctionId();

    /**
     * 功能参数
     *
     * @return FunctionParameter
     */
    List<FunctionParameter> getInputs();

    ThingFunctionInvokeMessage<R> addInput(FunctionParameter parameter);

    ThingFunctionInvokeMessage<R> functionId(String id);

    @Override
    R newReply();

    default MessageType getMessageType() {
        return MessageType.INVOKE_FUNCTION;
    }

    default Optional<Object> getInput(String name) {
        for (FunctionParameter input : getInputs()) {
            if (input.getName().equals(name)) {
                return Optional.ofNullable(input.getValue());
            }
        }
        return Optional.empty();
    }

    default Optional<Object> getInput(int index) {
        return getInputs().size() > index
                ? Optional.ofNullable(getInputs().get(index))
                : Optional.empty();
    }

    default Map<String, Object> inputsToMap() {
        List<FunctionParameter> inputs = getInputs();
        Map<String, Object> map = Maps.newLinkedHashMapWithExpectedSize(inputs.size());
        for (FunctionParameter input : inputs) {
            map.put(input.getName(), input.getValue());
        }
        return map;
    }

    default <T> T inputsToBean(Class<T> beanType) {
        return new JSONObject(inputsToMap())
                .toJavaObject(beanType);
    }

    default List<Object> inputsToList() {
        return getInputs()
                .stream()
                .map(FunctionParameter::getValue)
                .collect(Collectors.toList());
    }

    default Object[] inputsToArray() {
        return getInputs()
                .stream()
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

    @Override
    default void writeExternal(ObjectOutput out) throws IOException {
        RepayableThingMessage.super.writeExternal(out);
        SerializeUtils.writeNullableUTF(getFunctionId(), out);
        SerializeUtils.writeKeyValue(getInputs(),
                                     FunctionParameter::getName,
                                     FunctionParameter::getValue,
                                     out);
    }

    @Override
    default void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        RepayableThingMessage.super.readExternal(in);
        this.functionId(SerializeUtils.readNullableUTF(in));
        SerializeUtils.readKeyValue(in,this::addInput);
    }
}
