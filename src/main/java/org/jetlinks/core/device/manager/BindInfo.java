package org.jetlinks.core.device.manager;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jetlinks.core.utils.SerializeUtils;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

@Getter
@Setter
@AllArgsConstructor(staticName = "of")
@NoArgsConstructor
public class BindInfo implements Externalizable {
    private String key;
    private String deviceId;
    private String description;

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeUTF(key);
        out.writeUTF(deviceId);
        SerializeUtils.writeNullableUTF(description,out);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        this.key = in.readUTF();
        this.deviceId = in.readUTF();
        this.description = SerializeUtils.readNullableUTF(in);
    }
}