package org.jetlinks.core.collector;

import lombok.Getter;
import lombok.Setter;
import org.jetlinks.core.GenericHeaderSupport;
import org.jetlinks.core.utils.SerializeUtils;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 数采相关操作结果
 *
 * @param <T>
 */
@Getter
@Setter
public class Result<T> extends GenericHeaderSupport<Result<T>> implements Externalizable {

    /**
     * 数据
     */
    private T data;

    /**
     * 是否成功
     */
    private boolean success;

    /**
     * 状态码
     *
     * @see CollectorConstants.Codes
     */
    private int code;

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        SerializeUtils.writeKeyValue(getHeaders(), out);

        SerializeUtils.writeObject(data, out);
        out.writeBoolean(success);
        out.writeInt(code);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        setHeaders(SerializeUtils.readMap(in, ConcurrentHashMap::new));

        data = (T) SerializeUtils.readObject(in);

        success = in.readBoolean();
        code = in.readInt();
    }
}
