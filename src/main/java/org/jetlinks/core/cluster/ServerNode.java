package org.jetlinks.core.cluster;

import lombok.*;

import java.io.Serializable;
import java.util.Set;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServerNode implements Serializable {

    @NonNull
    private String id;

    private String name;

    private String host;

    private Set<String> tags;

    public boolean hasTag(String tag) {
        return tags != null && tags.contains(tag);
    }

    private long lastKeepAlive;

    public boolean isSame(ServerNode another) {
        return id.equals(another.getId());
    }

}
