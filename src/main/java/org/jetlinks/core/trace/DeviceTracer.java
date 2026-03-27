package org.jetlinks.core.trace;

import io.opentelemetry.api.common.AttributeKey;
import org.jetlinks.core.lang.SeparatedCharSequence;
import org.jetlinks.core.lang.SharedPathString;
import org.jetlinks.core.message.Message;
import org.jetlinks.core.utils.StringBuilderUtils;
import reactor.core.publisher.Mono;

public interface DeviceTracer {

    static  <R> MonoTracer<R> fromMessage(Message message) {
        return MonoTracer.createWith(message.getHeaders());
    }

    @SuppressWarnings("all")
    static <R extends Message> Mono<R> writeToMessage(R message) {
        return TraceHolder.writeContextTo(message, Message::addHeader);
    }

    interface SpanKey {

        // 设备ID
        AttributeKey<String> deviceId = AttributeKey.stringKey("deviceId");

        // 消息内容
        AttributeKey<String> message = AttributeKey.stringKey("message");

        // 响应信息
        AttributeKey<String> response = AttributeKey.stringKey("response");

        // 设备地址
        AttributeKey<String> address = AttributeKey.stringKey("address");

        // 输入报文
        AttributeKey<String> input = AttributeKey.stringKey("input"); //原始报文

        // 输出报文
        AttributeKey<String> output = AttributeKey.stringKey("output"); //编解码后的报文

        // 额外信息
        AttributeKey<String> tag = AttributeKey.stringKey("tag"); //额外信息

    }

    interface SpanName {

        SharedPathString all_operations = SharedPathString.of("/device/*/*");

        static SeparatedCharSequence operation0(String deviceId, String operation) {
            return all_operations.replace(2, deviceId, 3, operation);
        }

        static String operation(String deviceId, String operation) {
            return StringBuilderUtils
                    .buildString(deviceId, operation,
                                 (str, opt, stringBuilder) -> {
                                     stringBuilder
                                             .append("/device/")
                                             .append(str)
                                             .append("/")
                                             .append(opt);
                                 });
        }

        static String connection(String deviceId) {
            return operation(deviceId, OperationName.connection);
        }

        static SeparatedCharSequence connection0(String deviceId) {
            return operation0(deviceId, OperationName.connection);
        }

        static String auth(String deviceId) {
            return operation(deviceId, OperationName.auth);
        }

        static SeparatedCharSequence auth0(String deviceId) {
            return operation0(deviceId, OperationName.auth);
        }

        static String decode(String deviceId) {
            return operation(deviceId, OperationName.decode);
        }

        static SeparatedCharSequence decode0(String deviceId) {
            return operation0(deviceId, OperationName.decode);
        }

        static String encode(String deviceId) {
            return operation(deviceId, OperationName.encode);
        }

        static SeparatedCharSequence encode0(String deviceId) {
            return operation0(deviceId, OperationName.encode);
        }

        static String request(String deviceId) {
            return operation(deviceId, OperationName.request);
        }

        static SeparatedCharSequence request0(String deviceId) {
            return operation0(deviceId, OperationName.request);
        }

        static String response(String deviceId) {
            return operation(deviceId, OperationName.response);
        }

        static SeparatedCharSequence response0(String deviceId) {
            return operation0(deviceId, OperationName.response);
        }

        static String downstream(String deviceId) {
            return operation(deviceId, OperationName.downstream);
        }

        static SeparatedCharSequence downstream0(String deviceId) {
            return operation0(deviceId, OperationName.decode);
        }

        static String upstream(String deviceId) {
            return operation(deviceId, OperationName.upstream);
        }

        static SeparatedCharSequence upstream0(String deviceId) {
            return operation0(deviceId, OperationName.upstream);
        }

        static SeparatedCharSequence handle(String deviceId) {
            return operation0(deviceId, OperationName.handle);
        }
    }

    // 操作
    interface OperationName {

        // 连接
        String connection = "connection";

        // 设备认证
        String auth = "auth";

        // 数据上报
        String decode = "decode";

        // 数据下发
        String encode = "encode";

        // 请求
        String request = "request";

        // 响应
        String response = "response";

        // 下行
        String downstream = "downstream";

        // 上行
        String upstream = "upstream";

        // 处理设备消息
        String handle = "handle";
    }


}
