package org.jetlinks.core.message;

import com.alibaba.fastjson.JSONObject;

import java.util.Map;

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
    public Map<String, Object> getHeaders() {
        return null;
    }

    @Override
    public DeviceMessage addHeader(String header, Object value) {
       throw new UnsupportedOperationException();
    }

    @Override
    public JSONObject toJson() {
        return new JSONObject();
    }

    @Override
    public void fromJson(JSONObject jsonObject) {

    }

}
