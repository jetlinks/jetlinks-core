package org.jetlinks.core.message;

import org.jetlinks.core.message.exception.FunctionUndefinedException;
import org.jetlinks.core.message.exception.IllegalParameterException;
import org.jetlinks.core.message.exception.ParameterUndefinedException;
import org.jetlinks.core.message.function.FunctionInvokeMessageReply;
import org.jetlinks.core.message.function.FunctionParameter;
import org.jetlinks.core.metadata.PropertyMetadata;
import org.jetlinks.core.metadata.ValidateResult;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

/**
 * 调用设备功能的消息发送器
 *
 * @author zhouhao
 * @see org.jetlinks.core.device.DeviceMessageSender
 * @see org.jetlinks.core.message.function.FunctionInvokeMessage
 * @see FunctionInvokeMessageReply
 * @since 1.0.0
 */
public interface FunctionInvokeMessageSender {

    /**
     * 添加功能参数
     *
     * @param name  参数名 {@link FunctionParameter#getName()}
     * @param value 参数值 {@link FunctionParameter#getValue()}
     * @return this
     * @see FunctionParameter
     */
    FunctionInvokeMessageSender addParameter(String name, Object value);

    /**
     * 设置参数列表
     *
     * @param parameter 参数列表
     * @return this
     * @see FunctionParameter
     */
    FunctionInvokeMessageSender setParameter(List<FunctionParameter> parameter);

    /**
     * 将整个map设置到参数列表
     *
     * @param parameter map 参数
     * @return this
     */
    default FunctionInvokeMessageSender setParameter(Map<String, Object> parameter) {
        parameter.forEach(this::addParameter);
        return this;
    }

    /**
     * 指定messageId,如果不指定,将使用uuid生成一个.
     * <p>
     * ⚠️ messageId 应该全局唯一,且不能消息16位
     *
     * @param messageId messageId
     * @return this
     */
    FunctionInvokeMessageSender messageId(String messageId);

    /**
     * 设置调用此功能为异步执行, 当消息发送到设备后,立即返回{@link org.jetlinks.core.enums.ErrorCode#REQUEST_HANDLING},而不等待设备返回结果.
     *
     * @return this
     * @see org.jetlinks.core.message.function.FunctionInvokeMessage#setAsync(Boolean)
     */
    FunctionInvokeMessageSender async();

    /**
     * 执行参数校验
     *
     * @param resultConsumer 校验结果回调
     * @return this
     * @see PropertyMetadata#getValueType()
     * @see org.jetlinks.core.metadata.DataType#validate(Object)
     * @see IllegalArgumentException
     */
    FunctionInvokeMessageSender validate(BiConsumer<FunctionParameter, ValidateResult> resultConsumer)
            throws FunctionUndefinedException, ParameterUndefinedException, IllegalParameterException;

    /**
     * @return this
     * @see this#validate(BiConsumer)
     * @see IllegalArgumentException
     */
    default FunctionInvokeMessageSender validate() throws IllegalParameterException {
        validate((functionParameter, validateResult) -> validateResult.ifFail(r -> {
            throw new IllegalParameterException(functionParameter.getName(), validateResult.getErrorMsg());
        }));
        return this;
    }

    /**
     * 执行发送
     *
     * @return 异步完成阶段
     * @see org.jetlinks.core.device.DeviceMessageSender#send(RepayableDeviceMessage)
     * @see CompletionStage
     * @see CompletionStage#toCompletableFuture()
     * @see java.util.concurrent.CompletableFuture#get(long, TimeUnit)
     */
    CompletionStage<FunctionInvokeMessageReply> send();

}
