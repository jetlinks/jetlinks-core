package org.jetlinks.core.message.collector;

import lombok.Getter;
import lombok.Setter;
import org.jetlinks.core.utils.SerializeUtils;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

@Getter
@Setter
public class CollectorDataReply extends CollectorData {

    /**
     * 操作是否成功
     */
    private boolean success;

    /**
     * 失败原因
     */
    private String errorReason;

    public CollectorDataReply success(boolean success) {
        this.success = success;
        return this;
    }

    public CollectorDataReply error(String errorReason) {
        this.errorReason = errorReason;
        return this.success(false);
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        super.writeExternal(out);
        out.writeBoolean(success);
        SerializeUtils.writeObject(errorReason, out);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        super.readExternal(in);
        this.success = in.readBoolean();
        this.errorReason = (String) SerializeUtils.readObject(in);
    }
}
