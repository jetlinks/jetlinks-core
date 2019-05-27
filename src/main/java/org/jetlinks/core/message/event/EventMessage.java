package org.jetlinks.core.message.event;

import lombok.Getter;
import lombok.Setter;
import org.jetlinks.core.message.CommonDeviceMessageReply;

/**
 * @author zhouhao
 * @since 1.0.0
 */
@Getter
@Setter
public class EventMessage extends CommonDeviceMessageReply {

    private String event;

    private Object data;
}
