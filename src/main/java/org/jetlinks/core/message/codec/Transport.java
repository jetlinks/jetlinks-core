package org.jetlinks.core.message.codec;

public interface Transport {
    String getId();

    default String getName() {
        return getId();
    }

    default String getDescription() {
        return null;
    }

    default boolean isSame(Transport transport) {
        return this == transport || this.getId().equals(transport.getId());
    }

    default boolean isSame(String transportId) {
        return this.getId().equals(transportId);
    }
}
