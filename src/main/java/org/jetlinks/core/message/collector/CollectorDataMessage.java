package org.jetlinks.core.message.collector;

import lombok.Getter;
import lombok.Setter;
import org.jetlinks.core.message.CommonDeviceMessage;
import org.jetlinks.core.message.MessageType;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.List;

/**
 * 数采数据消息,通常用于数采类设备,如数采网关向平台传递采集到的数据.
 *
 * @author zhouhao
 * @since 1.2.1
 */
@Getter
@Setter
public class CollectorDataMessage extends CommonDeviceMessage<CollectorDataMessage> {

    /**
     * 数据列表
     */
    private List<CollectorData> data;

    @Override
    public final MessageType getMessageType() {
        return MessageType.COLLECTOR;
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        super.writeExternal(out);

        if (CollectionUtils.isEmpty(data)) {
            out.writeInt(0);
        } else {
            out.writeInt(data.size());
            for (CollectorData datum : data) {
                datum.writeExternal(out);
            }
        }
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        super.readExternal(in);

        int size = in.readInt();
        this.data = new ArrayList<>(size);

        for (int i = 0; i < size; i++) {
            CollectorData data = new CollectorData();
            data.readExternal(in);
            this.data.add(data);
        }
    }
}
