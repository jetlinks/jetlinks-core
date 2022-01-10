package org.jetlinks.core.things;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor(staticName = "of")
@EqualsAndHashCode
public class MetadataId {

    @NonNull
    private ThingMetadataType type;

    @NonNull
    private String id;

    @Override
    public String toString() {
        return type.name() + ":" + id;
    }
}
