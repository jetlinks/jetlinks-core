package org.jetlinks.core.utils;

import lombok.Getter;
import lombok.Setter;
import org.jetlinks.core.exception.CyclicDependencyException;
import org.junit.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class CyclicDependencyCheckerTest {

    @Test
    public void test() {
        Map<String, TestObj> objs = new HashMap<>();

        TestObj o1 = new TestObj();
        o1.setId("1");

        TestObj o2 = new TestObj();
        o2.setId("2");
        o2.setParentId(o1.getId());

        TestObj o3 = new TestObj();
        o3.setId("3");
        o3.setParentId(o2.getId());

        TestObj o4 = new TestObj();
        o4.setId("4");
        o4.setParentId(o3.getId());

        o1.setParentId(o4.getId());

        objs.put(o1.id,o1);
        objs.put(o2.id,o2);
        objs.put(o3.id,o3);
        objs.put(o4.id,o4);

        CyclicDependencyChecker<TestObj,Void> checker = CyclicDependencyChecker.of(TestObj::getId, TestObj::getParentId, id -> Mono.justOrEmpty(objs.get(id)));

        checker.check(o3)
                .as(StepVerifier::create)
                .expectError(CyclicDependencyException.class)
                .verify();

    }

    @Test
    public void testSelf() {
        Map<String, TestObj> objs = new HashMap<>();

        TestObj o1 = new TestObj();
        o1.setId("1");
        o1.setParentId("1");
        objs.put(o1.id,o1);
        CyclicDependencyChecker<TestObj,Void> checker = CyclicDependencyChecker.of(TestObj::getId, TestObj::getParentId, id -> Mono.justOrEmpty(objs.get(id)));

        checker.check(o1)
               .as(StepVerifier::create)
               .expectError(CyclicDependencyException.class)
               .verify();
    }


    @Getter
    @Setter
    static class TestObj {
        private String id;
        private String parentId;
    }

}