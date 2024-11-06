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
 * 读取数采点位数据消息,由平台下发给数采网关.数采网关需要回复{@link ReadCollectorDataMessageReply}消息给平台.
 *
 * @author zhouhao
 * @since 1.2.1
 * @see ReadCollectorDataMessageReply
 */
@Getter
@Setter
public final class ReadCollectorDataMessage extends CommonDeviceMessage<ReadCollectorDataMessage>
        implements RepayableDeviceMessage<ReadCollectorDataMessageReply> {

    /**
     * 要读取的地址列表
     */
    private List<String> addresses;

    @Override
    public MessageType getMessageType() {
        return MessageType.READ_COLLECTOR_DATA;
    }

    @Override
    public MessageType getReplyType() {
        return MessageType.READ_COLLECTOR_DATA_REPLY;
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        super.writeExternal(out);

        if (CollectionUtils.isEmpty(addresses)) {
            out.writeInt(0);
        } else {
            out.writeInt(addresses.size());
            for (String datum : addresses) {
                out.writeUTF(datum);
            }
        }
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        super.readExternal(in);

        int size = in.readInt();
        this.addresses = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            this.addresses.add(in.readUTF());
        }

    }

    @Override
    public ReadCollectorDataMessageReply newReply() {
        return new ReadCollectorDataMessageReply();
    }
}
