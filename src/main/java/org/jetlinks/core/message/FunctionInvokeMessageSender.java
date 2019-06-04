package org.jetlinks.core.message;

import io.vavr.control.Try;
import org.jetlinks.core.message.exception.FunctionUndefinedException;
import org.jetlinks.core.message.exception.IllegalParameterException;
import org.jetlinks.core.message.exception.ParameterUndefinedException;
import org.jetlinks.core.message.function.FunctionInvokeMessage;
import org.jetlinks.core.message.function.FunctionInvokeMessageReply;
import org.jetlinks.core.message.function.FunctionParameter;
import org.jetlinks.core.metadata.PropertyMetadata;
import org.jetlinks.core.metadata.ValidateResult;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

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
     * 自定义消息
     *
     * @param messageConsumer consumer
     * @return this
     */
    FunctionInvokeMessageSender custom(Consumer<FunctionInvokeMessage> messageConsumer);

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
     * ⚠️ messageId 应该全局唯一
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
    default FunctionInvokeMessageSender async() {
        return this.async(true);
    }

    /**
     * 设置是否异步
     *
     * @param async 是否异步
     * @return this
     * @see this#sync()
     * @see this#async(Boolean)
     */
    FunctionInvokeMessageSender async(Boolean async);

    /**
     * 设置调用此功能为同步返回, 当消息发送到设备后,将等待执行结果.
     *
     * @return this
     * @see org.jetlinks.core.message.function.FunctionInvokeMessage#setAsync(Boolean)
     */
    default FunctionInvokeMessageSender sync() {
        return this.async(false);
    }

    /**
     * 添加header到message中
     *
     * @param header header
     * @param value  值
     * @return this
     * @see DeviceMessage#addHeader(String, Object)
     */
    FunctionInvokeMessageSender header(String header, Object value);

    /**
     * 添加多个header到message中
     *
     * @param headers 多个headers
     * @return this
     * @see this#header(String, Object)
     * @see DeviceMessage#addHeader(String, Object)
     */
    default FunctionInvokeMessageSender headers(Map<String, Object> headers) {
        Objects.requireNonNull(headers)
                .forEach(this::header);
        return this;
    }

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

    /**
     * 尝试重新获取返回值
     *
     * @return 获取结果
     * @see org.jetlinks.core.device.DeviceMessageSender#retrieveReply(String, Supplier)
     * @see org.jetlinks.core.enums.ErrorCode#NO_REPLY
     */
    CompletionStage<FunctionInvokeMessageReply> retrieveReply();

    /**
     * 请看{@link this#retrieveReply()} 和 {@link Try}
     *
     * @param timeout  超时时间
     * @param timeUnit 超时时间单位
     * @return Try
     * @see this#retrieveReply()
     */
    default Try<FunctionInvokeMessageReply> tryRetrieveReply(long timeout, TimeUnit timeUnit) {
        return Try.of(() -> this.retrieveReply().toCompletableFuture().get(timeout, timeUnit));
    }

    /**
     * 发送消息并返回{@link Try},可进行函数式操作.
     *
     * <pre>
     *     sender.invokeFunction("test")
     *            .trySend(10,TimeUnit.SECONDS)
     *            .recoverWith(TimeoutException.class,r->failureTry(ErrorCode.TIME_OUT))
     *            .get();
     * </pre>
     *
     * @param timeout  超时时间
     * @param timeUnit 超时时间单位
     * @return Try
     */
    default Try<FunctionInvokeMessageReply> trySend(long timeout, TimeUnit timeUnit) {
        return Try.of(() -> {
            CompletableFuture<FunctionInvokeMessageReply> stage = send().toCompletableFuture();
            try {
                return stage.get(timeout, timeUnit);
            } catch (TimeoutException e) {
                //超时后取消执行任务,无法重新get.
                stage.cancel(true);
                throw e;
            }
        });
    }

    /**
     * 验证请求是否合法并发送消息并返回{@link Try},可进行函数式操作.
     *
     * <pre>
     *   FunctionInvokeMessageReply reply =  sender.invokeFunction("test")
     *            .tryValidateAndSend(10,TimeUnit.SECONDS)
     *            .recoverWith(TimeoutException.class,err->failureTry(ErrorCode.TIME_OUT))
     *            .recoverWith(IllegalParameterException,err->failureTry("PARAMETER_ERROR",err.getMessage()))
     *            .get();
     * </pre>
     *
     * @param timeout  超时时间
     * @param timeUnit 超时时间单位
     * @return Try
     */
    default Try<FunctionInvokeMessageReply> tryValidateAndSend(long timeout, TimeUnit timeUnit) {
        return Try.of(() -> {
            CompletableFuture<FunctionInvokeMessageReply> stage = validate().send().toCompletableFuture();
            try {
                return stage.get(timeout, timeUnit);
            } catch (TimeoutException e) {
                //超时后取消执行任务,无法重新get.
                stage.cancel(true);
                throw e;
            }
        });
    }


}
