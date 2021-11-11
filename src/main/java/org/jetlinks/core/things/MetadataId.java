package org.jetlinks.core.things;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor(staticName = "of")
@EqualsAndHashCode
public class MetadataId {

    private ThingMetadataType type;

    private String id;

}
