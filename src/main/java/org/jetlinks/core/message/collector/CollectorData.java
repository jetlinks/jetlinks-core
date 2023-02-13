package org.jetlinks.core.message.collector;

import lombok.*;
import org.jetlinks.core.utils.SerializeUtils;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

@Getter
@Setter
public class CollectorData implements Externalizable {
    private static final long serialVersionUID = 1L;

    /**
     * 采集器数据地址,如: /modbus/1/0/1 .
     */
    @NonNull
    private String address;

    /**
     * 采集器数据值
     */
    private Object value;

    /**
     * 采集器数据状态
     */
    private String state;

    /**
     * 数据时间戳,为null时以系统时间戳为准
     */
    private Long timestamp;

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        SerializeUtils.writeObject(address, out);
        SerializeUtils.writeObject(value, out);
        SerializeUtils.writeObject(state, out);
        SerializeUtils.writeObject(timestamp, out);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        address = (String) SerializeUtils.readObject(in);
        value = SerializeUtils.readObject(in);
        state = (String) SerializeUtils.readObject(in);
        timestamp = (Long) SerializeUtils.readObject(in);
    }
}
