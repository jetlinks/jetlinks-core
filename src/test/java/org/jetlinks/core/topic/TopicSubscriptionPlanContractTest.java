package org.jetlinks.core.topic;

import org.jetlinks.core.lang.SeparatedCharSequence;
import org.junit.Test;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.util.function.Consumer;

import static org.junit.Assert.*;

public class TopicSubscriptionPlanContractTest {

    @Test
    public void shouldValidateDecodeLimits() {
        TopicSubscriptionPlanDecodeLimits limits =
            new TopicSubscriptionPlanDecodeLimits(32, 50000, 256, 1048576);

        assertEquals(32, limits.getMaxRoutes());
        assertEquals(50000, limits.getMaxIndexedValues());
        assertEquals(256, limits.getMaxValueLength());
        assertEquals(1048576, limits.getMaxPayloadLength());

        assertInvalidLimits(0, 1, 1, 1);
        assertInvalidLimits(1, 0, 1, 1);
        assertInvalidLimits(1, 1, 0, 1);
        assertInvalidLimits(1, 1, 1, 0);
        assertInvalidLimits(-1, 1, 1, 1);
    }

    @Test
    public void shouldFreezeCodecSignature() throws IOException {
        TopicSubscriptionPlanCodec codec = new TopicSubscriptionPlanCodec() {
            @Override
            public int version() {
                return 1;
            }

            @Override
            public void encode(TopicSubscriptionPlan plan, DataOutput output) {
            }

            @Override
            public TopicSubscriptionPlan decode(
                DataInput input,
                int payloadLength,
                TopicSubscriptionPlanDecodeLimits limits) {
                return TopicSubscriptionPlan.empty();
            }
        };

        assertEquals(1, codec.version());
        assertSame(
            TopicSubscriptionPlan.empty(),
            codec.decode(
                new DataInputStream(new ByteArrayInputStream(new byte[0])),
                0,
                new TopicSubscriptionPlanDecodeLimits(1, 1, 1, 1)
            )
        );
    }

    @Test
    public void shouldFreezeRouteTableSignature() {
        TopicRouteTable<String> table = new TopicRouteTable<String>() {
            @Override
            public TopicRouteRegistration<String> register(
                String target,
                TopicSubscriptionPlan plan) {
                return null;
            }

            @Override
            public void find(
                SeparatedCharSequence topic,
                Consumer<? super TopicRouteRegistration<String>> consumer) {
            }

            @Override
            public void find(
                CharSequence topic,
                Consumer<? super TopicRouteRegistration<String>> consumer) {
            }

            @Override
            public TopicRouteTableMetrics metrics() {
                return null;
            }
        };

        assertNotNull(table);
    }

    private static void assertInvalidLimits(int routes,
                                            int values,
                                            int valueLength,
                                            int payloadLength) {
        assertFails(
            IllegalArgumentException.class,
            () -> new TopicSubscriptionPlanDecodeLimits(
                routes,
                values,
                valueLength,
                payloadLength
            )
        );
    }

    private static void assertFails(Class<? extends Throwable> expected, Runnable action) {
        try {
            action.run();
            fail("expected " + expected.getName());
        } catch (Throwable error) {
            assertTrue("unexpected error: " + error, expected.isInstance(error));
        }
    }
}
