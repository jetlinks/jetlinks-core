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
 * 数采数据消息上报,如数采网关向平台传递采集到的数据.平台再对此类数据进行处理,如转换为设备物模型数据等.
 *
 * @author zhouhao
 * @since 1.2.1
 */
@Getter
@Setter
public final class ReportCollectorDataMessage extends CommonDeviceMessage<ReportCollectorDataMessage> {

    /**
     * 数据列表
     */
    private List<CollectorData> data;

    @Override
    public MessageType getMessageType() {
        return MessageType.REPORT_COLLECTOR;
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
