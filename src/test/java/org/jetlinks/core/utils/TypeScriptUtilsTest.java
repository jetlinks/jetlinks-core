package org.jetlinks.core.utils;

import org.junit.Test;

import java.util.Arrays;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class TypeScriptUtilsTest {

    @Test
    public void test() {
        String script = TypeScriptUtils.loadDeclare("transparent-codec");

        System.out.println(Arrays.stream(script.split("\n"))
                        .map(str-> str.replace("\"","\\\""))
                .collect(Collectors.joining("\"\n,\"","[\"","\"]")));

    }
}