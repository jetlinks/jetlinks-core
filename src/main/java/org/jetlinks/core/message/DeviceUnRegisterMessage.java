package org.jetlinks.core.message;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DeviceUnRegisterMessage extends CommonDeviceMessage {

    private String childrenDeviceId;

}
