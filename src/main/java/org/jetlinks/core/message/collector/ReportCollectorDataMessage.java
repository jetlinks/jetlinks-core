package org.jetlinks.core.message.collector;

import lombok.Getter;
import lombok.Setter;
import org.jetlinks.core.message.CommonDeviceMessage;
import org.jetlinks.core.message.HeaderKey;
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
     * 采集器地址信息,通过在消息头中设置此信息,可以指定数据来源地址,如: /modbus/1/0/1.
     *
     * <pre>{@code
     *  //当设置了此header为'/modbus/1/0/1'时,平台将推送到以下topic.
     *  /device/{productId}/{deviceId}/message/collector/report/modbus/1/0/1
     * }</pre>
     * <p>
     * 在平台订阅时,需要指定对应的地址,或者使用/**通配符.
     */
    public static final HeaderKey<String> ADDRESS = HeaderKey.of("_address", null, String.class);

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
