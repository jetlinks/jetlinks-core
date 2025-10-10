package org.jetlinks.core.server.session;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.jetlinks.core.device.*;
import org.jetlinks.core.device.session.DeviceSessionEvent;
import org.jetlinks.core.device.session.DeviceSessionInfo;
import org.jetlinks.core.device.session.DeviceSessionManager;
import org.jetlinks.core.message.codec.DefaultTransport;
import org.jetlinks.core.message.codec.EncodedMessage;
import org.jetlinks.core.message.codec.Transport;
import org.junit.Before;
import org.junit.Test;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Duration;
import java.util.function.Function;
import java.util.function.Predicate;

import static org.junit.Assert.*;

public class MultiGatewayDeviceSessionProviderTest {

    private TestDeviceRegistry registry;
    private StandaloneDeviceMessageBroker messageBroker;
    private DeviceSessionManager sessionManager;
    private MultiGatewayDeviceSession.Provider provider;

    @Before
    public void init() {
        messageBroker = new StandaloneDeviceMessageBroker();
        registry = new TestDeviceRegistry(new TestProtocolSupport(), messageBroker);
        
        // 创建一个简单的 DeviceSessionManager 实现
        sessionManager = new DeviceSessionManager() {
            @Override
            public String getCurrentServerId() {
                return "test-server";
            }

            @Override
            public Mono<DeviceSession> compute(@Nonnull String deviceId,
                                               @Nonnull Function<Mono<DeviceSession>, Mono<DeviceSession>> computer) {
                return Mono.empty();
            }

            @Override
            public Mono<DeviceSession> compute(@Nonnull String deviceId,
                                               @Nullable Mono<DeviceSession> creator,
                                               @Nullable Function<DeviceSession, Mono<DeviceSession>> updater) {
                return Mono.empty();
            }

            @Override
            public Mono<DeviceSession> getSession(String deviceId) {
                return Mono.empty();
            }

            @Override
            public Mono<DeviceSession> getSession(String deviceId, boolean unregisterWhenNotAlive) {
                return Mono.empty();
            }

            @Override
            public Flux<DeviceSession> getSessions() {
                return Flux.empty();
            }

            @Override
            public Mono<Long> remove(String deviceId, boolean onlyLocal) {
                return Mono.just(0L);
            }

            @Override
            public Mono<Long> remove(String deviceId, Predicate<DeviceSession> predicate) {
                return Mono.just(0L);
            }

            @Override
            public Mono<Boolean> isAlive(String deviceId, boolean onlyLocal) {
                return Mono.just(false);
            }

            @Override
            public Mono<Boolean> checkAlive(String deviceId, boolean onlyLocal) {
                return Mono.just(false);
            }

            @Override
            public Mono<Long> totalSessions(boolean onlyLocal) {
                return Mono.just(0L);
            }

            @Override
            public Flux<DeviceSessionInfo> getSessionInfo() {
                return Flux.empty();
            }

            @Override
            public Flux<DeviceSessionInfo> getDeviceSessionInfo(String deviceId) {
                return Flux.empty();
            }

            @Override
            public Flux<DeviceSessionInfo> getSessionInfo(String serverId) {
                return Flux.empty();
            }

            @Override
            public Flux<DeviceSessionInfo> getLocalSessionInfo() {
                return Flux.empty();
            }

            @Override
            public Disposable listenEvent(Function<DeviceSessionEvent, Mono<Void>> handler) {
                return () -> {};
            }
        };
        
        provider = new MultiGatewayDeviceSession.Provider(sessionManager);
    }

    @Test
    public void testGetId() {
        assertEquals("multi_gateway", provider.getId());
    }

    @Test
    public void testSerializeAndDeserialize() {
        // 注册设备
        DeviceInfo deviceInfo = new DeviceInfo();
        deviceInfo.setId("test-device-001");
        deviceInfo.setProtocol("test");

        registry.register(deviceInfo)
                .flatMap(device -> {
                    // 创建 MultiGatewayDeviceSession
                    MultiGatewayDeviceSession multiSession = new MultiGatewayDeviceSession(device, sessionManager);
                    
                    // 创建并注册 KeepOnlineSession（可序列化的会话）
                    LostDeviceSession lostSession = new LostDeviceSession("session-1", device, DefaultTransport.TCP);
                    KeepOnlineSession testSession = new KeepOnlineSession(lostSession, Duration.ofMinutes(5));
                    multiSession.register(testSession);

                    // 序列化
                    return provider.serialize(multiSession, registry)
                            .flatMap(serialized -> {
                                assertNotNull(serialized);
                                assertTrue(serialized.length > 0);
                                
                                // 反序列化
                                return provider.deserialize(serialized, registry);
                            })
                            .doOnNext(deserializedSession -> {
                                assertNotNull(deserializedSession);
                                assertEquals("test-device-001", deserializedSession.getDeviceId());
                                assertTrue(deserializedSession instanceof MultiGatewayDeviceSession);
                                
                                MultiGatewayDeviceSession multiGatewaySession = (MultiGatewayDeviceSession) deserializedSession;
                                // 验证会话数量
                                assertTrue(multiGatewaySession.size() > 0);
                            });
                })
                .as(StepVerifier::create)
                .expectNextCount(1)
                .verifyComplete();
    }

    @Test
    public void testSerializeEmptySession() {
        // 注册设备
        DeviceInfo deviceInfo = new DeviceInfo();
        deviceInfo.setId("test-device-002");
        deviceInfo.setProtocol("test");

        registry.register(deviceInfo)
                .flatMap(device -> {
                    // 创建空的 MultiGatewayDeviceSession（没有子会话）
                    MultiGatewayDeviceSession multiSession = new MultiGatewayDeviceSession(device, sessionManager);
                    
                    // 序列化空会话
                    return provider.serialize(multiSession, registry);
                })
                .as(StepVerifier::create)
                .expectNextMatches(serialized -> {
                    assertNotNull(serialized);
                    assertTrue(serialized.length > 0);
                    
                    // 验证序列化数据包含设备ID
                    // 注意：序列化时第一个字段是Provider ID（"multi_gateway"）而不是设备ID
                    ByteBuf buf = Unpooled.wrappedBuffer(serialized);
                    try {
                        int providerIdLength = buf.readUnsignedShort();
                        assertTrue(providerIdLength > 0);
                        byte[] providerIdBytes = new byte[providerIdLength];
                        buf.readBytes(providerIdBytes);
                        String providerId = new String(providerIdBytes);
                        // 序列化数据的第一个字段是设备ID，这里应该是"test-device-002"
                        assertEquals("test-device-002", providerId);
                        return true;
                    } finally {
                        buf.release();
                    }
                })
                .verifyComplete();
    }

    @Test
    public void testDeserializeInvalidData() {
        // 测试反序列化无效数据
        byte[] invalidData = new byte[]{0x00, 0x01}; // 太短的数据
        
        provider.deserialize(invalidData, registry)
                .as(StepVerifier::create)
                .expectError(IndexOutOfBoundsException.class)
                .verify();
    }

    @Test
    public void testDeserializeWithNonExistentDevice() {
        // 创建包含不存在设备ID的序列化数据
        ByteBuf buf = Unpooled.buffer();
        byte[] deviceId = "non-existent-device".getBytes();
        buf.writeShort(deviceId.length);
        buf.writeBytes(deviceId);
        
        byte[] serialized = new byte[buf.readableBytes()];
        buf.readBytes(serialized);
        buf.release();
        
        provider.deserialize(serialized, registry)
                .as(StepVerifier::create)
                .expectNextCount(0)
                .verifyComplete();
    }

    /**
     * 测试用的简单 DeviceSession 实现
     */
    private static class TestDeviceSession implements DeviceSession, PersistentSession {
        private final String id;
        private final DeviceOperator operator;
        private final Transport transport;
        private final long connectTime;
        private Duration keepAliveTimeout = Duration.ofMinutes(5);

        public TestDeviceSession(String id, DeviceOperator operator, Transport transport) {
            this.id = id;
            this.operator = operator;
            this.transport = transport;
            this.connectTime = System.currentTimeMillis();
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public String getDeviceId() {
            return operator.getDeviceId();
        }

        @Override
        public DeviceOperator getOperator() {
            return operator;
        }

        @Override
        public long lastPingTime() {
            return connectTime;
        }

        @Override
        public long connectTime() {
            return connectTime;
        }

        @Override
        public Mono<Boolean> send(EncodedMessage encodedMessage) {
            return Mono.just(true);
        }

        @Override
        public Transport getTransport() {
            return transport;
        }

        @Override
        public void close() {
        }

        @Override
        public void ping() {
        }

        @Override
        public boolean isAlive() {
            return true;
        }

        @Override
        public void onClose(Runnable call) {
        }

        @Override
        public String getProvider() {
            return "test";
        }

        @Override
        public Duration getKeepAliveTimeout() {
            return keepAliveTimeout;
        }

        @Override
        public void setKeepAliveTimeout(Duration timeout) {
            this.keepAliveTimeout = timeout;
        }
    }
}

