package org.jetlinks.core.message;

import com.alibaba.fastjson.JSONObject;
import lombok.Getter;
import lombok.Setter;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * 透传设备消息
 *
 * @author zhouhao
 * @since 1.0.2
 */
@Getter
@Setter
public class DirectDeviceMessage extends CommonDeviceMessage<DirectDeviceMessage> {

    private byte[] payload;

    @Override
    public MessageType getMessageType() {
        return MessageType.DIRECT;
    }

    @Override
    public void fromJson(JSONObject jsonObject) {
        setPayload(jsonObject.getBytes("payload"));
        setDeviceId(jsonObject.getString("deviceId"));
        setMessageId(jsonObject.getString("messageId"));
        Long ts = jsonObject.getLong("timestamp");
        if (null != ts) {
            setTimestamp(ts);
        }
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        super.writeExternal(out);
        if (payload == null) {
            out.writeInt(0);
        } else {
            out.writeInt(payload.length);
            out.write(payload);
        }
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        super.readExternal(in);
        int len = in.readInt();
        if (len > 0) {
            payload = new byte[len];
            in.readFully(payload);
        }
    }
}
