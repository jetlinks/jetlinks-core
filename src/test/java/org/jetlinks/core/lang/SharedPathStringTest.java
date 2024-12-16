package org.jetlinks.core.lang;

import lombok.SneakyThrows;
import org.jetlinks.core.utils.SerializeUtils;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import static org.junit.Assert.*;

public class SharedPathStringTest {


    @Test
    public void test() {
        String topic = "/test/1/2/3";

        SharedPathString sharedPathString = SharedPathString.of(topic);

        assertEquals(topic, sharedPathString.toString());
        assertEquals(topic.length(), sharedPathString.length());
        assertEquals(topic.charAt(0), sharedPathString.charAt(0));
        assertEquals(topic.substring(1, 2), sharedPathString.subSequence(1, 2).toString());

        System.out.println(sharedPathString.hashCode());

        assertEquals(0, sharedPathString.compareTo(SharedPathString.of(topic)));
        assertEquals(sharedPathString, SharedPathString.of(topic));
        assertEquals(sharedPathString.hashCode(), SharedPathString.of(topic).hashCode());

        assertEquals(sharedPathString,codec(sharedPathString));
    }

    @SneakyThrows
    public Object codec(Object obj) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (ObjectOutputStream objOut = new ObjectOutputStream(output)) {
            SerializeUtils.writeObject(obj, objOut);
        }
        ByteArrayInputStream input = new ByteArrayInputStream(output.toByteArray());
        try (ObjectInputStream obIn = new ObjectInputStream(input)) {
            return SerializeUtils.readObject(obIn);
        }
    }
}