package org.jetlinks.core.message;

import com.alibaba.fastjson.JSONObject;

/**
 * @author zhouhao
 * @since 1.0.0
 */
public class EmptyDeviceMessage implements DeviceMessage {

    public static final EmptyDeviceMessage INSTANCE = new EmptyDeviceMessage();

    private EmptyDeviceMessage() {
    }

    @Override
    public String getMessageId() {
        return null;
    }

    @Override
    public String getDeviceId() {
        return null;
    }

    @Override
    public long getTimestamp() {
        return 0;
    }

    @Override
    public JSONObject toJson() {
        return new JSONObject();
    }

    @Override
    public void fromJson(JSONObject jsonObject) {

    }

}
