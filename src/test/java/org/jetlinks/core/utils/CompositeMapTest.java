package org.jetlinks.core.utils;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import lombok.SneakyThrows;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CompositeMapTest {
    @Test
    @SneakyThrows
    public void testSerializable() {
        CompositeMap<String,String> map = new CompositeMap<>(
                new HashMap<>(Maps.asMap(Sets.newHashSet("1","2","3"),String::valueOf)),
                new HashMap<>(Maps.asMap(Sets.newHashSet("a","b","c"),String::valueOf))
        );

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ObjectOutputStream oout = new ObjectOutputStream(out)) {
            oout.writeObject(map);
        }

        try (ObjectInputStream oin = new ObjectInputStream(new ByteArrayInputStream(out.toByteArray()))) {
            Object read = oin.readObject();
            assertTrue(read instanceof Map);
            System.out.println(read);
        }
    }

    @Test
    public void test(){
        {
            CompositeMap<String,String> map = new CompositeMap<>(
                    Maps.asMap(Sets.newHashSet("1","2","3"),String::valueOf),
                    Maps.asMap(Sets.newHashSet("a","b","c"),String::valueOf)
            );
            assertEquals(6,map.size());
            assertEquals(6,map.values().size());
            assertEquals(6,map.keySet().size());

            assertEquals(6,new HashMap<>(map).size());

            assertEquals("a",map.get("a"));
            assertEquals("2",map.get("2"));
        }

        {
            CompositeMap<String,String> map = new CompositeMap<>(
                    Maps.asMap(Sets.newHashSet("1","2","3"),String::valueOf),
                    Maps.asMap(Sets.newHashSet("a","b","c","1"),String::valueOf)
            );
            System.out.println(map);
            assertEquals(6,map.size());
            assertEquals(6,new HashMap<>(map).size());
            assertEquals(6,map.values().size());
            assertEquals(6,map.keySet().size());

        }

        {
            CompositeMap<String,String> map = new CompositeMap<>(
                    Maps.asMap(Sets.newHashSet("1","2","3"),String::valueOf),
                    Maps.asMap(Sets.newHashSet("1","2","3","4"),String::valueOf)
            );
            assertEquals(4,map.size());
            assertEquals(4,new HashMap<>(map).size());
            assertEquals(4,map.values().size());
            System.out.println(map.values());
            assertEquals(4,map.keySet().size());

        }

        {
            CompositeMap<String,String> map = new CompositeMap<>(
                    Maps.asMap(Sets.newHashSet("1","2","a"),String::valueOf),
                    Maps.asMap(Sets.newHashSet("1","2","3","4"),String::valueOf)
            );
            assertEquals(5,map.size());
            assertEquals(5,new HashMap<>(map).size());
            System.out.println(map.values());
            assertEquals(5,map.values().size());
            assertEquals(5,map.keySet().size());

        }

        {
            CompositeMap<String,String> map = new CompositeMap<>(
                    Maps.asMap(Sets.newHashSet("1","2","3","4"),String::valueOf),
                    Maps.asMap(Sets.newHashSet("1","2","a"),String::valueOf)

            );
            assertEquals(5,map.size());
            assertEquals(5,new HashMap<>(map).size());
            System.out.println(map.values());
            assertEquals(5,map.values().size());
            assertEquals(5,map.keySet().size());

        }
    }
}