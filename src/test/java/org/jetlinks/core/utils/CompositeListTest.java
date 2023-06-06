package org.jetlinks.core.utils;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class CompositeListTest {


    @Test
    public void test() {

        List<Integer> list = new CompositeList<>(
                Arrays.asList(1, 2, 3),
                Arrays.asList(4, 5, 6, 7)
        );

        assertEquals(7, list.size());

        {
            //测试foreach
            long it = 0;
            for (Integer integer : list) {
                it++;
            }
            assertEquals(7, it);
        }

        //sublist
        {
            List<Integer> sub = list.subList(1, 5);
            System.out.println(sub);
            assertEquals(4, sub.size());

        }
        //list it
        {
            ListIterator<Integer> iterator = list.listIterator();
            assertTrue(iterator.hasNext());
            assertFalse(iterator.hasPrevious());

            for (int i = 0; i < list.size(); i++) {

                assertEquals(i, iterator.nextIndex());

                assertEquals(Integer.valueOf(i + 1), iterator.next());

            }

            assertFalse(iterator.hasNext());

            for (int i = list.size(); i > 0; i--) {
                assertTrue(iterator.hasPrevious());
                assertEquals(i - 1, iterator.previousIndex());
                assertEquals(Integer.valueOf(i), iterator.previous());
            }
            assertTrue(iterator.hasNext());
            assertFalse(iterator.hasPrevious());


        }
    }

}