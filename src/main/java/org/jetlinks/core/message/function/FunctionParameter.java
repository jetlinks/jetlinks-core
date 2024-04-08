package org.jetlinks.core.message.function;

import lombok.*;
import org.jetlinks.core.utils.SerializeUtils;

import java.io.*;

/**
 * @author zhouhao
 * @since 1.0.0
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class FunctionParameter implements Externalizable {
    private static final long serialVersionUID = -6849794470754667710L;

    @NonNull
    private String name;

    private Object value;

    @Override
    public String toString() {
        return  name+"("+value+")";
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeUTF(name);
        SerializeUtils.writeObject(value,out);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        name = in.readUTF();
        value = SerializeUtils.readObject(in);
    }
}
