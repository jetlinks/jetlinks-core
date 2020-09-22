package org.jetlinks.core.message.property;

import com.alibaba.fastjson.JSONObject;
import lombok.Getter;
import lombok.Setter;
import org.jetlinks.core.message.CommonDeviceMessageReply;
import org.jetlinks.core.message.MessageType;

import java.util.Map;

/**
 * 读取设备属性消息回复, 方向: 设备->平台
 * <p>
 * 在设备接收到{@link ReadPropertyMessage}消息后,使用此消息进行回复,回复后,指令发起方将收到响应结果.
 *
 * @author zhouhao
 * @since 1.0.0
 */
@Getter
@Setter
public class ReadPropertyMessageReply extends CommonDeviceMessageReply<ReadPropertyMessageReply> {

    /**
     * 回复的属性,key为物模型中的属性ID,value为物模型对应的类型值.
     * <p>
     * 注意: value如果是结构体(对象类型),请勿传入在协议包中自定义的对象,应该转为{@link Map}传入.
     */
    private Map<String, Object> properties;

    public static ReadPropertyMessageReply create() {
        ReadPropertyMessageReply reply = new ReadPropertyMessageReply();

        reply.setTimestamp(System.currentTimeMillis());

        return reply;
    }

    public ReadPropertyMessageReply success(Map<String, Object> properties) {

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
        return MessageType.READ_PROPERTY_REPLY;
    }

}
