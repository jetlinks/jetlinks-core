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

        AttributeKey<String> deviceId = AttributeKey.stringKey("deviceId");

        AttributeKey<String> message = AttributeKey.stringKey("message");

        AttributeKey<String> response = AttributeKey.stringKey("response");

        AttributeKey<String> address = AttributeKey.stringKey("address");

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
            return operation(deviceId, "connection");
        }

        static SeparatedCharSequence connection0(String deviceId) {
            return operation0(deviceId, "connection");
        }

        static String auth(String deviceId) {
            return operation(deviceId, "auth");
        }

        static SeparatedCharSequence auth0(String deviceId) {
            return operation0(deviceId, "auth");
        }

        static String decode(String deviceId) {
            return operation(deviceId, "decode");
        }

        static SeparatedCharSequence decode0(String deviceId) {
            return operation0(deviceId, "decode");
        }

        static String encode(String deviceId) {
            return operation(deviceId, "encode");
        }

        static SeparatedCharSequence encode0(String deviceId) {
            return operation0(deviceId, "encode");
        }

        static String request(String deviceId) {
            return operation(deviceId, "request");
        }

        static SeparatedCharSequence request0(String deviceId) {
            return operation0(deviceId, "request");
        }

        static String response(String deviceId) {
            return operation(deviceId, "response");
        }

        static SeparatedCharSequence response0(String deviceId) {
            return operation0(deviceId, "response");
        }

        static String downstream(String deviceId) {
            return operation(deviceId, "downstream");
        }

        static SeparatedCharSequence downstream0(String deviceId) {
            return operation0(deviceId, "downstream");
        }

        static String upstream(String deviceId) {
            return operation(deviceId, "upstream");
        }

        static SeparatedCharSequence upstream0(String deviceId) {
            return operation0(deviceId, "upstream");
        }
    }


}
