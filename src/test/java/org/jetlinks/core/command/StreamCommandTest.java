package org.jetlinks.core.command;


import org.junit.Assert;
import org.junit.Test;
import reactor.core.publisher.Flux;

import javax.annotation.Nonnull;

public class StreamCommandTest   {


    @Test
    public void testConvert(){
        StreamCommand<Integer,String> cmd= new StreamCommand<Integer, String>() {
            @Nonnull
            @Override
            public Flux<Integer> stream() {
                return Flux.empty();
            }

            @Override
            public void withStream(@Nonnull Flux<Integer> stream) {

            }
        };

        Assert.assertEquals(Integer.valueOf(1), cmd.convertStreamValue("1"));
    }
}