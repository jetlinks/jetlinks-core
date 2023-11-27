package org.jetlinks.core.command;

import lombok.EqualsAndHashCode;
import org.jetlinks.core.utils.SerializeUtils;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@EqualsAndHashCode(of = "properties")
public abstract class AbstractCommand<Response, Self extends AbstractCommand<Response, Self>>
        implements Command<Response>, Externalizable {

    private Map<String, Object> properties;

    @Override
    public final Self with(String key, Object value) {
        writable().put(key, value);
        return castSelf();
    }

    @Override
    public final Self with(Map<String, Object> properties) {
        writable().putAll(properties);
        return castSelf();
    }

    public Map<String, Object> readable() {
        return properties == null ? Collections.emptyMap() : properties;
    }

    public Map<String, Object> writable() {
        return properties == null ? properties = new HashMap<>() : properties;
    }

    @SuppressWarnings("all")
    protected Self castSelf() {
        return (Self) this;
    }


    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        SerializeUtils.writeKeyValue(properties, out);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        SerializeUtils.readKeyValue(in,writable()::put);
    }
}
