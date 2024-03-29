package org.jetlinks.core.command;

import org.junit.Test;
import reactor.core.publisher.Flux;

import static org.junit.Assert.*;

public class CommandUtilsTest {

    @Test
    public void test() {

        System.out.println(CommandUtils.getCommandResponseType(new TestCommand()));
        assertTrue(CommandUtils.commandResponsePublisher(new TestCommand()));
        assertTrue(CommandUtils.commandResponseFlux(new TestCommand()));
        assertFalse(CommandUtils.commandResponseMono(new TestCommand()));


        assertEquals(String.class,CommandUtils.getCommandResponseDataType(new TestCommand()).toClass());

        TestCommand cmd = new  TestCommand();

        assertEquals("test", cmd.createResponseData("test"));
        assertEquals("123", cmd.createResponseData(123));

    }


    public static class TestCommand extends AbstractCommand<Flux<String>,TestCommand> {

    }
}