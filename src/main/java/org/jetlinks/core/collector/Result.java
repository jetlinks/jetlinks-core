package org.jetlinks.core.collector;

import lombok.Getter;
import lombok.Setter;
import org.jetlinks.core.GenericHeaderSupport;
import org.jetlinks.core.utils.ExceptionUtils;
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


    public Result<T> withPointId(String pointId){
        return addHeader(CollectorConstants.Headers.pointId,pointId);
    }

    public static Result<Void> success() {
        return success(null);
    }

    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setSuccess(true);
        result.setData(data);
        result.setCode(CollectorConstants.Codes.success);
        return result;
    }

    public static <T> Result<T> error(int code) {
        Result<T> result = new Result<>();
        result.setSuccess(false);
        result.setCode(code);
        return result;
    }

    public static <T> Result<T> error(int code, Throwable error) {
        Result<T> result = new Result<>();
        result.setSuccess(false);
        result.addHeader("errorType", error.getClass().getCanonicalName());
        result.addHeader("errorStack", ExceptionUtils.getStackTrace(error));
        result.setCode(code);
        return result;
    }

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
