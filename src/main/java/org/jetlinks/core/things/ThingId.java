package org.jetlinks.core.things;

import lombok.*;
import org.springframework.util.DigestUtils;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.nio.charset.StandardCharsets;

@Getter
@Setter
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor(staticName = "of")
public class ThingId implements Externalizable {

    @NonNull
    private String type;

    @NonNull
    private String id;

    public String toUniqueId() {
        byte[] typeBytes = type.getBytes(StandardCharsets.UTF_8);
        byte[] idBytes = id.getBytes(StandardCharsets.UTF_8);
        byte[] arr = new byte[typeBytes.length + idBytes.length];
        System.arraycopy(typeBytes, 0, arr, 0, typeBytes.length);
        System.arraycopy(idBytes, 0, arr, typeBytes.length, idBytes.length);
        return DigestUtils.md5DigestAsHex(arr);
    }

    @Override
    public String toString() {
        return type + ":" + id;
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeUTF(type);
        out.writeUTF(id);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        type = in.readUTF();
        id = in.readUTF();
    }
}
