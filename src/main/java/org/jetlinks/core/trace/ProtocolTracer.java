package org.jetlinks.core.trace;

import org.jetlinks.core.utils.StringBuilderUtils;

public interface ProtocolTracer {


    interface SpanName {

        static String operation(String protocolId, String operation) {
            return StringBuilderUtils
                .buildString(protocolId, operation,
                             (str, opt, stringBuilder) -> stringBuilder
                                 .append("/protocol/")
                                 .append(str)
                                 .append("/")
                                 .append(opt));
        }

        static String encode(String protocolId) {
            return operation(protocolId, "encode");
        }

        static String decode(String protocolId) {
            return operation(protocolId, "decode");
        }

        static String install(String protocolId) {
            return operation(protocolId, "install");
        }


    }
}
