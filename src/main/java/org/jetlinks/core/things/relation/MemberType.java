package org.jetlinks.core.things.relation;

import org.jetlinks.core.metadata.Metadata;
import org.jetlinks.core.metadata.PropertyMetadata;

import java.util.List;

public interface MemberType extends Metadata {

    List<PropertyMetadata> getProperties();

    List<Relation> getRelations(String anotherMemberType);
}
