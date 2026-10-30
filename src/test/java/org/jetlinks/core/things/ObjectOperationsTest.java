package org.jetlinks.core.things;

import org.jetlinks.core.command.CommandException;
import org.jetlinks.core.metadata.PropertyMetadata;
import org.jetlinks.core.metadata.SimplePropertyMetadata;
import org.jetlinks.core.metadata.types.StringType;
import org.jetlinks.core.things.relation.ObjectType;
import org.jetlinks.core.things.relation.PropertyOperation;
import org.jetlinks.core.things.relation.Relation;
import org.jetlinks.core.things.relation.RelationObject;
import org.jetlinks.core.things.relation.RelationOperation;
import org.junit.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class ObjectOperationsTest {
    @Test
    public void should_keep_legacy_objects_and_types_usable() {
        RelationObject object = new RelationObject() {
            public String getId() { return "D1"; }
            public String getType() { return "device"; }
            public RelationOperation relations(boolean reverse) { throw new UnsupportedOperationException(); }
            public PropertyOperation properties() {
                return key -> Mono.deferContextual(context -> Mono.just(context.get("caller")));
            }
        };
        StepVerifier.create(object.getMetadata()).verifyComplete();
        StepVerifier.create(object.commands().getCommandMetadata("missing")).verifyComplete();
        StepVerifier.create(object.commands().commandIsSupported("missing")).expectNext(false).verifyComplete();
        StepVerifier.create(object.commands().executeToFlux("control", Collections.emptyMap()))
            .expectError(CommandException.class).verify();
        Mono<Object> read = object.properties().get("name");
        StepVerifier.create(read.contextWrite(context -> context.put("caller", "first")))
            .expectNext("first").verifyComplete();
        StepVerifier.create(read.contextWrite(context -> context.put("caller", "second")))
            .expectNext("second").verifyComplete();

        ObjectType type = new ObjectType() {
            public String getId() { return "device"; }
            public String getName() { return "设备"; }
            public String getDescription() { return ""; }
            public Map<String, Object> getExpands() { return Collections.emptyMap(); }
            public List<PropertyMetadata> getProperties() {
                return Collections.singletonList(SimplePropertyMetadata.of("id", "ID", StringType.GLOBAL));
            }
            public List<Relation> getRelations(String target) { return Collections.emptyList(); }
            public List<ObjectType> getRelatedTypes() { return Collections.emptyList(); }
        };
        assertFalse(type instanceof ThingMetadata);
        assertEquals("id", type.getProperties().get(0).getId());
        assertTrue(type.getRelations().isEmpty());
        assertFalse(type.hasRelation("user", "owner"));
    }
}
