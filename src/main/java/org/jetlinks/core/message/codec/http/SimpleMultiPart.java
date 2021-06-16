package org.jetlinks.core.message.codec.http;

import java.util.*;

class SimpleMultiPart implements MultiPart {
    private final List<Part> parts;

    public SimpleMultiPart(List<Part> parts) {
        this.parts = parts;
    }

    @Override
    public Optional<Part> getPart(String name) {
        return parts
                .stream()
                .filter(part -> Objects.equals(name, part.getName()))
                .findAny();
    }

    @Override
    public List<Part> getParts() {
        return parts;
    }

    @Override
    public String toString() {
        return "MultiPart{" +
                "parts=" + parts +
                '}';
    }
}
