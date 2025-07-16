package org.jetlinks.core.collector;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.codec.binary.Hex;
import org.jetlinks.core.utils.SerializeUtils;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 点位数据
 *
 * @author zhouhao
 * @since 1.2.3
 */
@Getter
@Setter
public class PointData implements Externalizable {

    /**
     * 点位ID
     *
     * @see PointProperties#getId()
     * @see DataCollectorProvider.PointRuntime#getId()
     */
    private String id;

    /**
     * 点位状态标识
     */
    private String state;

    /**
     * 点位原始数据
     */
    private byte[] originData;

    /**
     * 解析后的数据
     */
    private Object parsedData;

    /**
     * 时间戳
     */
    private long timestamp;

    /**
     * 其他自定义数据
     */
    private Map<String, Object> others;

    @Override
    public String toString() {
        String val = parsedData == null ? Hex.encodeHexString(originData) : String.valueOf(parsedData);
        return state==null?val:(val + ":" + state);
    }


    public PointData withOther(String key, Object value) {
        othersWriter().put(key, value);
        return this;
    }

    private Map<String, Object> othersWriter() {
        return others == null ? others = new ConcurrentHashMap<>() : others;
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        SerializeUtils.writeNullableUTF(id, out);
        SerializeUtils.writeNullableUTF(state, out);
        SerializeUtils.writeObject(originData, out);

        SerializeUtils.writeObject(parsedData, out);
        out.writeLong(timestamp);

        SerializeUtils.writeKeyValue(others, out);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        id = SerializeUtils.readNullableUTF(in);
        state = SerializeUtils.readNullableUTF(in);
        originData = SerializeUtils.readObjectAs(in);

        parsedData = SerializeUtils.readObject(in);
        timestamp = in.readLong();

        SerializeUtils.readKeyValue(in, this::withOther);
    }
}
