package org.jetlinks.core.things.relation;

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
public class SimpleObjectProperty implements ObjectProperty, Externalizable {
    private String property;
    private Object value;

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeUTF(property);
        SerializeUtils.writeObject(value, out);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        property = in.readUTF();
        value = SerializeUtils.readObject(in);
    }
}
