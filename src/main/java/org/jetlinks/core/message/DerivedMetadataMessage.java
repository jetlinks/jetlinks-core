package org.jetlinks.core.message;

import lombok.Getter;
import lombok.Setter;
import org.jetlinks.core.utils.SerializeUtils;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

@Getter
@Setter
public class DerivedMetadataMessage extends CommonDeviceMessage<DerivedMetadataMessage> {

    //物模型数据
    private String metadata;

    //是否是全量数据
    private boolean all;

    @Override
    public MessageType getMessageType() {
        return MessageType.DERIVED_METADATA;
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        super.writeExternal(out);
        SerializeUtils.writeNullableUTF(metadata,out);
        out.writeBoolean(all);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        super.readExternal(in);
        metadata = SerializeUtils.readNullableUTF(in);
        all = in.readBoolean();
    }
}
