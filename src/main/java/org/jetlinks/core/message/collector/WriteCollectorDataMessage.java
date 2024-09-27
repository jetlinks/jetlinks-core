package org.jetlinks.core.message.collector;

import lombok.Getter;
import lombok.Setter;
import org.jetlinks.core.message.CommonDeviceMessage;
import org.jetlinks.core.message.MessageType;
import org.jetlinks.core.message.RepayableDeviceMessage;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.List;

/**
 *  写入数采点位数据消息,由平台下发给数采网关进行点位修改操作,数采网关需要回复{@link WriteCollectorDataMessageReply}消息给平台.
 *
 * @author zhouhao
 * @since 1.2.1
 * @see WriteCollectorDataMessageReply
 */
@Getter
@Setter
public final class WriteCollectorDataMessage extends CommonDeviceMessage<WriteCollectorDataMessage>
        implements RepayableDeviceMessage<WriteCollectorDataMessageReply> {

    /**
     * 数据列表
     */
    private List<CollectorData> data;

    @Override
    public MessageType getMessageType() {
        return MessageType.WRITE_COLLECTOR_DATA;
    }

    @Override
    public MessageType getReplyType() {
        return MessageType.WRITE_COLLECTOR_DATA_REPLY;
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

    @Override
    public WriteCollectorDataMessageReply newReply() {
        return new WriteCollectorDataMessageReply();
    }
}
