package org.jetlinks.core.message.property;

import com.alibaba.fastjson.JSONObject;
import lombok.Getter;
import lombok.Setter;
import org.jetlinks.core.message.CommonDeviceMessageReply;
import org.jetlinks.core.message.MessageType;

import java.util.Map;

/**
 * 上报设备属性
 *
 * @author zhouhao
 * @since 1.0.0
 */
@Getter
@Setter
public class ReportPropertyMessage extends CommonDeviceMessageReply<ReportPropertyMessage> {

    private Map<String, Object> properties;

    public static ReportPropertyMessage create() {
        ReportPropertyMessage reply = new ReportPropertyMessage();

        reply.setTimestamp(System.currentTimeMillis());

        return reply;
    }

    public ReportPropertyMessage success(Map<String, Object> properties) {

        this.properties = properties;
        super.setSuccess(true);
        return this;

    }

    @Override
    public void fromJson(JSONObject jsonObject) {
        super.fromJson(jsonObject);
        this.properties = jsonObject.getJSONObject("properties");
    }

    public MessageType getMessageType() {
        return MessageType.REPORT_PROPERTY;
    }

}
