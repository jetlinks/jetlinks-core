package org.jetlinks.core.message.property;

import lombok.Getter;
import lombok.Setter;
import org.jetlinks.core.message.CommonThingMessage;
import org.jetlinks.core.message.MessageType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 读取设备属性消息, 方向: 平台->设备
 * <p>
 * 下发指令后,设备需要回复指令{@link ReadPropertyMessageReply}
 *
 * @author zhouhao
 * @see ReadPropertyMessageReply
 * @since 1.0.0
 */
@Getter
@Setter
public class DefaultReadPropertyMessage extends CommonThingMessage<DefaultReadPropertyMessage> implements ReadThingPropertyMessage<DefaultReadPropertyMessageReply> {

    /**
     * 要读取的属性列表,协议包可根据实际情况处理此参数,
     * 有的设备可能不支持读取指定的属性,则直接读取全部属性返回即可
     */
    private List<String> properties = new ArrayList<>();

    public DefaultReadPropertyMessage addProperties(List<String> properties) {
        this.properties.addAll(properties);
        return this;
    }

    public DefaultReadPropertyMessage addProperties(String... properties) {
        return addProperties(Arrays.asList(properties));
    }

    @Override
    public DefaultReadPropertyMessageReply newReply() {
        return new DefaultReadPropertyMessageReply()
                .from(this);
    }

    public MessageType getMessageType() {
        return MessageType.READ_PROPERTY;
    }

}
