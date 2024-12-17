package org.jetlinks.core.trace;

import org.jetlinks.core.lang.SharedPathString;
import org.jetlinks.core.utils.StringBuilderUtils;

public interface ProtocolTracer {


    interface SpanName {

        SharedPathString allOperations = SharedPathString.of("/protocol/*/*");

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

        static CharSequence encode0(String protocolId) {
            return allOperations.replace(2, protocolId, 3, "encode");
        }

        static String decode(String protocolId) {
            return operation(protocolId, "decode");
        }

        static CharSequence decode0(String protocolId) {
            return allOperations.replace(2, protocolId, 3, "decode");
        }

        static String install(String protocolId) {
            return operation(protocolId, "install");
        }


    }
}
