package org.jetlinks.core.cluster;

import lombok.*;

import java.io.Serializable;
import java.util.Map;
import java.util.Optional;

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

    private Map<String, Object> tags;

    private long lastKeepAlive;

    public boolean hasTag(String tag) {
        return tags != null && tags.containsKey(tag);
    }

    public Optional<Object> getTag(String tag) {
        return Optional.ofNullable(tags)
                .map(t -> t.get(tag));
    }

    public boolean isSame(ServerNode another) {
        return id.equals(another.getId());
    }

}
