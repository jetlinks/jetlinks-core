package org.jetlinks.core.message;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DerivedMetadataMessage extends CommonDeviceMessage {

    //元数据
    private String metadata;

    //是否是全量数据
    private boolean all;

    @Override
    public MessageType getMessageType() {
        return MessageType.DERIVED_METADATA;
    }
}
