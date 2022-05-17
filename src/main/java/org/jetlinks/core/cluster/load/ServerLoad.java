package org.jetlinks.core.cluster.load;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

@Getter
@Setter
@AllArgsConstructor(staticName = "of")
@NoArgsConstructor
public class ServerLoad implements Externalizable {
    private static final long serialVersionUID = 1;

    private String serverNodeId;

    private String feature;

    private long load;

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeUTF(serverNodeId);
        out.writeUTF(feature);
        out.writeLong(load);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        serverNodeId = in.readUTF();
        feature = in.readUTF();
        load = in.readLong();
    }
}
