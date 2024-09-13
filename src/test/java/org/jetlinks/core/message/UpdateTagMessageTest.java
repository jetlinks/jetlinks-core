package org.jetlinks.core.message;

import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.*;

public class UpdateTagMessageTest {


    @Test
    public void testNull(){
        UpdateTagMessage message = new UpdateTagMessage();
        message.tag("null",null);
        message.tags(Collections.singletonMap("null",null));

    }
}