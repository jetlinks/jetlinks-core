package org.jetlinks.core.trace;

import io.opentelemetry.api.common.AttributeKey;
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

        AttributeKey<String> deviceId = AttributeKey.stringKey("deviceId");

        AttributeKey<String> message = AttributeKey.stringKey("message");

        AttributeKey<String> response = AttributeKey.stringKey("response");

        AttributeKey<String> address = AttributeKey.stringKey("address");

    }

    interface SpanName {

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
            return operation(deviceId, "connection");
        }

        static String auth(String deviceId) {
            return operation(deviceId, "auth");
        }

        static String decode(String deviceId) {
            return operation(deviceId, "decode");
        }

        static String encode(String deviceId) {
            return operation(deviceId, "encode");
        }

        static String request(String deviceId) {
            return operation(deviceId, "request");
        }

        static String response(String deviceId) {
            return operation(deviceId, "response");
        }

        static String downstream(String deviceId) {
            return operation(deviceId, "downstream");
        }

        static String upstream(String deviceId) {
            return operation(deviceId, "upstream");
        }

    }


}
