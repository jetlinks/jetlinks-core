package org.jetlinks.core.defaults;

import lombok.extern.slf4j.Slf4j;
import org.jetlinks.core.device.DeviceOperator;
import org.jetlinks.core.message.FunctionInvokeMessageSender;
import org.jetlinks.core.message.Headers;
import org.jetlinks.core.exception.MetadataUndefinedException;
import org.jetlinks.core.message.exception.FunctionUndefinedException;
import org.jetlinks.core.message.exception.IllegalParameterException;
import org.jetlinks.core.message.function.FunctionInvokeMessage;
import org.jetlinks.core.message.function.FunctionInvokeMessageReply;
import org.jetlinks.core.message.function.FunctionParameter;
import org.jetlinks.core.metadata.PropertyMetadata;
import org.jetlinks.core.metadata.ValidateResult;
import org.jetlinks.core.utils.IdUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;


@Slf4j
public class DefaultFunctionInvokeMessageSender implements FunctionInvokeMessageSender {

    private final FunctionInvokeMessage message = new FunctionInvokeMessage();

    private final DeviceOperator operator;

    public DefaultFunctionInvokeMessageSender(DeviceOperator operator, String functionId) {
        this.operator = operator;
        message.setMessageId(IdUtils.newUUID());
        message.setFunctionId(functionId);
        message.setDeviceId(operator.getDeviceId());
    }

    @Override
    public FunctionInvokeMessageSender custom(Consumer<FunctionInvokeMessage> messageConsumer) {
        messageConsumer.accept(message);
        return this;
    }

    @Override
    public FunctionInvokeMessageSender addParameter(FunctionParameter parameter) {
        message.addInput(parameter);
        return this;
    }

    @Override
    public FunctionInvokeMessageSender setParameter(List<FunctionParameter> parameter) {
        message.setInputs(new ArrayList<>(parameter));
        return this;
    }

    /**
     * 添加消息ID
     *
     * @param messageId 消息ID
     * @return 调用设备功能的消息发送器
     */
    @Override
    public FunctionInvokeMessageSender messageId(String messageId) {
        message.setMessageId(messageId);
        return this;
    }

    /**
     * 添加消息头
     *
     * @param header 消息头标识
     * @param value 消息头存储的值
     * @return 调用设备功能的消息发送器
     */
    @Override
    public FunctionInvokeMessageSender header(String header, Object value) {
        message.addHeader(header, value);
        return this;
    }

    /**
     * 执行参数校验
     * <pre>
     *     function("door-open")
     *     .validate()
     *     .flatMany(FunctionInvokeMessageSender::send)
     *     .doOnError(IllegalParameterException.class,err->log.error(err.getMessage(),err))
     *     ...
     * </pre>
     *
     * @see Mono#doOnError(Consumer)
     * @see org.jetlinks.core.message.exception.IllegalParameterException
     * @see org.jetlinks.core.message.exception.FunctionUndefinedException
     * @see org.jetlinks.core.message.exception.FunctionIllegalParameterException
     */
    @Override
    public Mono<FunctionInvokeMessageSender> validate() {
        String function = message.getFunctionId();

        return operator
                .getMetadata()
                .switchIfEmpty(Mono.error(() -> new MetadataUndefinedException(operator.getDeviceId())))
                .flatMap(metadata -> Mono
                        .justOrEmpty(metadata.getFunction(function))
                        .switchIfEmpty(Mono.error(() -> new FunctionUndefinedException(function)))
                )
                .doOnNext(functionMetadata -> {
                    List<PropertyMetadata> metadataInputs = functionMetadata.getInputs();
                    List<FunctionParameter> inputs = message.getInputs();

                    Map<String, FunctionParameter> properties = inputs
                            .stream()
                            .collect(Collectors.toMap(FunctionParameter::getName, Function.identity(), (t1, t2) -> t1));
                    for (PropertyMetadata metadata : metadataInputs) {
                        FunctionParameter parameter = properties.get(metadata.getId());
                        Object value = Optional
                                .ofNullable(parameter)
                                .map(FunctionParameter::getValue)
                                .orElse(null);
                        if (value == null) {
                            continue;
                        }

                        ValidateResult validateResult = metadata.getValueType().validate(value);

                        validateResult.ifFail(result -> {
                            throw new IllegalParameterException(metadata.getId(), result.getErrorMsg());
                        });
                        if (validateResult.getValue() != null) {
                            parameter.setValue(validateResult.getValue());
                        }
                    }
                })
                .thenReturn(this)
                ;
    }

    /**
     * 发送消息
     *
     * @return 返回结果
     * @see org.jetlinks.core.exception.DeviceOperationException
     * @see org.jetlinks.core.enums.ErrorCode#CLIENT_OFFLINE
     */
    @Override
    public Flux<FunctionInvokeMessageReply> send() {
        if (message.getHeader(Headers.async).isPresent()) {
            return doSend();
        }
        return operator
                .getMetadata()
                .flatMap(meta -> Mono.justOrEmpty(meta.getFunction(message.getFunctionId())))
                .doOnNext(func -> async(func.isAsync()))
                .thenMany(doSend());
    }

    /**
     * 发送消息
     *
     * @return 回复内容
     */
    private Flux<FunctionInvokeMessageReply> doSend() {
        return operator
                .messageSender()
                .send(Mono.just(message));
    }
}
