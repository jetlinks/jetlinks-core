package org.jetlinks.core.message;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DeviceRegisterMessage extends CommonDeviceMessage {

    private String childrenDeviceId;

}
