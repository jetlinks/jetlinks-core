package org.jetlinks.core.message.property;

import com.alibaba.fastjson.JSONObject;
import lombok.Getter;
import lombok.Setter;
import org.jetlinks.core.message.CommonDeviceMessageReply;

import java.util.Map;

/**
 * @author zhouhao
 * @since 1.0.0
 */
@Getter
@Setter
public class ReadPropertyMessageReply extends CommonDeviceMessageReply<ReadPropertyMessageReply> {

    private Map<String, Object> properties;

    public static ReadPropertyMessageReply create(){
        return new ReadPropertyMessageReply();
    }

    @Override
    public void fromJson(JSONObject jsonObject) {
        super.fromJson(jsonObject);
        this.properties=jsonObject.getJSONObject("properties");
    }
}
